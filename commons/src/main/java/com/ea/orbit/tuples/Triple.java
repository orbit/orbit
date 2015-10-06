package com.ea.orbit.tuples;

public class Triple<L, M, R>
{
    private final L left;
    private final M middle;
    private final R right;

    protected Triple(final L left, final M middle, final R right)
    {
        this.left = left;
        this.middle = middle;
        this.right = right;
    }

    public static <L, M, R> Triple<L, M, R> of(L left, M middle, R right)
    {
        return new Triple<>(left, middle, right);
    }

    public L getLeft()
    {
        return left;
    }

    public R getRight()
    {
        return right;
    }

    public M getMiddle()
    {
        return middle;
    }

    @Override
    public boolean equals(final Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        final Triple<?, ?, ?> triple = (Triple<?, ?, ?>) o;

        if (left != null ? !left.equals(triple.left) : triple.left != null) return false;
        if (middle != null ? !middle.equals(triple.middle) : triple.middle != null) return false;
        return !(right != null ? !right.equals(triple.right) : triple.right != null);

    }

    @Override
    public int hashCode()
    {
        int result = left != null ? left.hashCode() : 0;
        result = 31 * result + (middle != null ? middle.hashCode() : 0);
        result = 31 * result + (right != null ? right.hashCode() : 0);
        return result;
    }
}
