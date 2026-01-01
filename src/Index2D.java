package assignments.Ex3;

public class Index2D implements Pixel2D {
    private final int __x, __y; //the object should not change(immutable)

    /**
     * Constructor of index in the matrix.
     * @param x the X coordinate of the index.
     * @param y the Y coordinate of the index.
     */
    public Index2D(int x, int y) {
        this.__x =x;
        this.__y =y;
    }
    /**Default constructor of index in the matrix.*/
    public Index2D() {
        this(0,0);
    }

    /**
     * Copy constructor
     * @throws RuntimeException if other==null.
     * @param other index(point) to copy.
     * */
    public Index2D(Pixel2D other) {
        if (other == null) throw new RuntimeException("other is null");
        this.__x = other.getX();
        this.__y = other.getY();
    }
    /**
     * @return the X coordinate (integer) of the pixel.
     */
    @Override
    public int getX() {
        return __x;
    }
    /**
     * @return the Y coordinate (integer) of the pixel.
     */
    @Override
    public int getY() {
        return __y;
    }
    /**
     * This method computes the 2D (Euclidean) distance beteen this pixel and p2 pixel, i.e., (Math.sqrt(dx*dx+dy*dy))
     * @throws RuntimeException if p2==null.
     * @return the 2D Euclidean distance between the pixels.
     */
    @Override
    public double distance2D(Pixel2D p2) {
        if (p2 == null) throw new RuntimeException("p2 is null");
        int deltaX = this.getX() - p2.getX();
        int deltaY = this.getY() - p2.getY();
        return Math.sqrt((double)deltaX * deltaX + (double)deltaY * deltaY);
    }
    /**@return a String representation of this coordinate.*/
    @Override
    public String toString() {
        return getX()+","+getY();
    }
    /**
     * @param t the reference object with which to compare.
     * @return true if and only if this Index is the same index as p.
     */
    @Override
    public boolean equals(Object t) {
        boolean ans = false;
        if(t instanceof Pixel2D) {
            Pixel2D p = (Pixel2D) t;
            ans = (this.distance2D(p)==0);
        }
        return ans;
    }
}
