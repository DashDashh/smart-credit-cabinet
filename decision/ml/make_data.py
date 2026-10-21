"""Генерация синтетических данных и сохранение в CSV."""
import argparse
from pathlib import Path

from generator import generate


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--n", type=int, default=20_000)
    parser.add_argument("--seed", type=int, default=42)
    parser.add_argument("--noise", type=float, default=1.0)
    parser.add_argument("--intercept", type=float, default=-4.7)
    parser.add_argument("--out", type=str, default="data/synthetic.csv")
    args = parser.parse_args()

    df = generate(
        n=args.n, seed=args.seed, noise=args.noise, intercept=args.intercept
    )
    out = Path(args.out)
    out.parent.mkdir(parents=True, exist_ok=True)
    df.to_csv(out, index=False)

    print(f"Сохранено: {out} ({len(df)} строк)")
    print(f"Доля дефолтов: {df['default'].mean():.4f}")


if __name__ == "__main__":
    main()