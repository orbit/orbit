package com.ea.orbit.tuples;

public class Pair<L, R>
{
    private final L left;
    private final R right;

    protected Pair(final L left, final R right)
    {
        this.left = left;
        this.right = right;
    }

    public static <L, R> Pair<L, R> of(L left, R right)
    {
        return new Pair<>(left, right);
    }

    public L getLeft()
    {
        return left;
    }

    public R getRight()
    {
        return right;
    }

    @Override
    public boolean equals(final Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        final Pair<?, ?> pair = (Pair<?, ?>) o;

        if (left != null ? !left.equals(pair.left) : pair.left != null) return false;
        return !(right != null ? !right.equals(pair.right) : pair.right != null);

    }

    @Override
    public int hashCode()
    {
        int result = left != null ? left.hashCode() : 0;
        result = 31 * result + (right != null ? right.hashCode() : 0);
        return result;
    }
}
