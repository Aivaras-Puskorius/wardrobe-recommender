"""Train the garment classifier in app/src/main/assets/models from the CC0 clothing dataset.

The dataset is github.com/alexeygrigorev/clothing-dataset (CC0 1.0): about 5,400 photos of single
garments, most laid flat, in 20 classes. This script merges them into the classes the app can use,
fine-tunes MobileNetV2 (ImageNet weights) on them, and writes an int8 TensorFlow Lite model with
uint8 input, which ML Kit's custom image labeler runs without metadata, plus a labels file in the same
order as the model's outputs.

MobileNetV2 rather than the newer MobileNetV3: the int8 MobileNetV3-Large model was rejected by XNNPACK,
TensorFlow Lite's CPU accelerator ("failed to prepare"), while MobileNetV2 runs with it.

Setup, outside the repo (TensorFlow needs Python 3.10 to 3.13):
    uv venv --python 3.12 ~/.cache/clankdrobe-training/venv
    VIRTUAL_ENV=~/.cache/clankdrobe-training/venv uv pip install tensorflow==2.21.0 pillow numpy
    curl -L -o clothing.zip https://github.com/alexeygrigorev/clothing-dataset/archive/refs/heads/master.zip
    unzip clothing.zip

Run from the repo root:
    ~/.cache/clankdrobe-training/venv/bin/python tools/train_garment_model.py PATH/TO/clothing-dataset-master
"""

import argparse
import collections
import csv
import random
from pathlib import Path

import numpy as np
import tensorflow as tf

# Dataset class -> app class. Classes left out ("Not sure", "Skip") are not used at all.
# "other" teaches the model what is not a garment the app knows, so it can decline to suggest one.
CLASSES = {
    "t_shirt": ["T-Shirt", "Polo", "Undershirt", "Top"],
    "shirt": ["Shirt", "Blouse"],
    "sweater": ["Longsleeve", "Hoodie"],
    "pants": ["Pants"],
    "shorts": ["Shorts"],
    "skirt": ["Skirt"],
    "shoes": ["Shoes"],
    "outerwear": ["Outwear", "Blazer"],
    "other": ["Dress", "Hat", "Body", "Other"],
}
LABELS = list(CLASSES)
SIZE = 224
SEED = 7
OUT_DIR = Path(__file__).resolve().parent.parent / "app/src/main/assets/models"
MODEL_NAME = "garments_mobilenetv2_int8.tflite"
LABELS_NAME = "garments_labels.txt"


def load_rows(dataset: Path):
    """(image path, class index, sender) for every photo in a used class."""
    to_class = {source: LABELS.index(target) for target, sources in CLASSES.items() for source in sources}
    rows = []
    with open(dataset / "images.csv") as f:
        for row in csv.DictReader(f):
            if row["label"] in to_class:
                rows.append((str(dataset / "images" / f"{row['image']}.jpg"), to_class[row["label"]], row["sender_id"]))
    return rows


def split_by_sender(rows):
    """80/10/10 split that keeps each contributor's photos in one part, so the test set has unseen people."""
    senders = sorted({sender for _, _, sender in rows})
    random.Random(SEED).shuffle(senders)
    counts = collections.Counter(sender for _, _, sender in rows)
    parts, total, done = {}, len(rows), 0
    for sender in senders:
        share = done / total
        parts[sender] = "train" if share < 0.8 else "val" if share < 0.9 else "test"
        done += counts[sender]
    return {part: [r for r in rows if parts[r[2]] == part] for part in ("train", "val", "test")}


def dataset_of(rows, batch, shuffle):
    paths = [p for p, _, _ in rows]
    labels = [c for _, c, _ in rows]

    def load(path, label):
        image = tf.io.decode_jpeg(tf.io.read_file(path), channels=3)
        image = tf.image.resize(image, (SIZE, SIZE), antialias=True)
        return tf.cast(tf.clip_by_value(image, 0, 255), tf.uint8), label

    ds = tf.data.Dataset.from_tensor_slices((paths, labels)).map(load, num_parallel_calls=tf.data.AUTOTUNE).cache()
    if shuffle:
        ds = ds.shuffle(len(paths), seed=SEED)
    return ds.batch(batch).prefetch(tf.data.AUTOTUNE)


def build_model():
    base = tf.keras.applications.MobileNetV2(input_shape=(SIZE, SIZE, 3), include_top=False, weights="imagenet")
    base.trainable = False
    # The model takes pixels from 0 to 255, so the app passes the photo as it is. MobileNetV2 wants -1 to 1.
    inputs = tf.keras.Input((SIZE, SIZE, 3))
    x = tf.keras.layers.Rescaling(1 / 127.5, offset=-1)(inputs)
    x = base(x, training=False)
    x = tf.keras.layers.GlobalAveragePooling2D()(x)
    x = tf.keras.layers.Dropout(0.3)(x)
    outputs = tf.keras.layers.Dense(len(LABELS), activation="softmax")(x)
    return tf.keras.Model(inputs, outputs), base


def augment():
    return tf.keras.Sequential([
        tf.keras.layers.RandomFlip("horizontal"),
        tf.keras.layers.RandomRotation(0.08),
        tf.keras.layers.RandomZoom(0.15),
        tf.keras.layers.RandomContrast(0.2),
        tf.keras.layers.RandomBrightness(0.2, value_range=(0, 255)),
    ])


def report(name, probabilities, truth):
    predicted = probabilities.argmax(1)
    confidence = probabilities.max(1)
    print(f"\n{name}: accuracy {np.mean(predicted == truth):.3f} on {len(truth)} photos")
    for threshold in (0.0, 0.5, 0.6, 0.7, 0.8):
        kept = confidence >= threshold
        if kept.any():
            print(f"  confidence >= {threshold:.1f}: suggests for {kept.mean():.0%}, right {np.mean(predicted[kept] == truth[kept]):.0%}")
    matrix = np.zeros((len(LABELS), len(LABELS)), dtype=int)
    for t, p in zip(truth, predicted):
        matrix[t, p] += 1
    print("  rows are the truth, columns the prediction:")
    print("  " + " " * 10 + " ".join(f"{label[:6]:>6}" for label in LABELS))
    for i, label in enumerate(LABELS):
        print(f"  {label:10s}" + " ".join(f"{n:6d}" for n in matrix[i]))


def main():
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("dataset", type=Path, help="the unzipped clothing-dataset-master folder")
    parser.add_argument("--head-epochs", type=int, default=8)
    parser.add_argument("--tune-epochs", type=int, default=6)
    parser.add_argument("--work-dir", type=Path, default=Path.home() / ".cache/clankdrobe-training",
                        help="where the trained Keras model is kept between runs")
    parser.add_argument("--skip-training", action="store_true", help="convert the Keras model from the last run")
    args = parser.parse_args()
    keras_path = args.work_dir / "garments.keras"
    tf.keras.utils.set_random_seed(SEED)

    parts = split_by_sender(load_rows(args.dataset))
    for part, rows in parts.items():
        counts = collections.Counter(LABELS[c] for _, c, _ in rows)
        print(part, len(rows), dict(counts))

    train = dataset_of(parts["train"], 32, shuffle=True)
    val = dataset_of(parts["val"], 64, shuffle=False)
    test = dataset_of(parts["test"], 64, shuffle=False)
    augmenter = augment()
    train_augmented = train.map(lambda x, y: (augmenter(tf.cast(x, tf.float32), training=True), y))
    as_float = lambda x, y: (tf.cast(x, tf.float32), y)  # noqa: E731

    counts = collections.Counter(c for _, c, _ in parts["train"])
    weights = {c: len(parts["train"]) / (len(LABELS) * counts[c]) for c in counts}

    if args.skip_training:
        model = tf.keras.models.load_model(keras_path)
    else:
        model, base = build_model()
        model.compile(tf.keras.optimizers.Adam(1e-3), "sparse_categorical_crossentropy", metrics=["accuracy"])
        model.fit(train_augmented, validation_data=val.map(as_float), epochs=args.head_epochs, class_weight=weights)

        # Fine-tune the last blocks. BatchNorm layers stay frozen, as Keras recommends for small datasets.
        base.trainable = True
        for layer in base.layers[:-60]:
            layer.trainable = False
        for layer in base.layers:
            if isinstance(layer, tf.keras.layers.BatchNormalization):
                layer.trainable = False
        model.compile(tf.keras.optimizers.Adam(1e-5), "sparse_categorical_crossentropy", metrics=["accuracy"])
        model.fit(train_augmented, validation_data=val.map(as_float), epochs=args.tune_epochs, class_weight=weights)
        args.work_dir.mkdir(parents=True, exist_ok=True)
        model.save(keras_path)

    truth = np.array([c for _, c, _ in parts["test"]])
    report("Keras model, test set", model.predict(test.map(as_float), verbose=0), truth)

    def representative():
        for images, _ in train.unbatch().batch(1).take(300):
            yield [tf.cast(images, tf.float32)]

    converter = tf.lite.TFLiteConverter.from_keras_model(model)
    converter.optimizations = [tf.lite.Optimize.DEFAULT]
    converter.representative_dataset = representative
    converter.target_spec.supported_ops = [tf.lite.OpsSet.TFLITE_BUILTINS_INT8]
    converter.inference_input_type = tf.uint8
    converter.inference_output_type = tf.uint8
    tflite = converter.convert()
    OUT_DIR.mkdir(parents=True, exist_ok=True)
    (OUT_DIR / MODEL_NAME).write_bytes(tflite)
    (OUT_DIR / LABELS_NAME).write_text("\n".join(LABELS) + "\n")
    print(f"\nwrote {OUT_DIR / MODEL_NAME} ({len(tflite) / 1e6:.1f} MB) and {LABELS_NAME}")

    # The default interpreter applies XNNPACK, as phones do, so this also checks the model runs with it.
    interpreter = tf.lite.Interpreter(model_content=tflite)
    interpreter.allocate_tensors()
    input_index = interpreter.get_input_details()[0]["index"]
    output = interpreter.get_output_details()[0]
    scale, zero = output["quantization"]
    probabilities = []
    for images, _ in test.unbatch().batch(1):
        interpreter.set_tensor(input_index, images.numpy())
        interpreter.invoke()
        probabilities.append((interpreter.get_tensor(output["index"])[0].astype(np.float32) - zero) * scale)
    report("int8 TensorFlow Lite model, test set", np.array(probabilities), truth)


if __name__ == "__main__":
    main()
