package assignments.Ex3;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;

public class Map implements Map2D {
	private int[][] _map;
	private boolean _cyclicFlag = true;

	/**
	 * Construct a 2D w*h matrix of integers.
	 * @param w the width of the underlying 2D array.
	 * @param h the height of the underlying 2D array.
	 * @param v the init value of all the entries in the 2D array.
	 */
	public Map(int w, int h, int v) {init(w,h, v);}
	/**
	 * Constructs a square map (size*size).
	 * @param size width,height for create map
	 */
	public Map(int size) {this(size,size, 0);}
	/**
	 * Constructs a map from a given 2D array.
	 * @param data Map to copy
	 */
	public Map(int[][] data) {
		init(data);
	}
	/**
	 * Construct a 2D w*h matrix of integers.
	 * @param w the width of the underlying 2D array.
	 * @param h the height of the underlying 2D array.
	 * @param v the init value of all the entries in the 2D array.
	 * @throws RuntimeException if width/height illegal
	 */
	@Override
	public void init(int w, int h, int v) {
		if (w <= 0 || h <= 0) throw new RuntimeException("Illegal size");
		_map = new int[w][h];
		for (int x = 0; x < w; x++) Arrays.fill(_map[x], v);
	}
	/**
	 * Constructs a 2D raster map from a given 2D int array (deep copy).
	 * @throws RuntimeException if arr == null or if the array is empty or a ragged 2D array.
	 * @param arr a 2D int array.
	 */
	@Override
	public void init(int[][] arr) {
		if (arr == null)
			throw new RuntimeException("arr is null");

		if (arr.length == 0)
			throw new RuntimeException("empty array");

		if (arr[0] == null || arr[0].length == 0)
			throw new RuntimeException("empty row");

		int h = arr[0].length;
		for (int i = 1; i < arr.length; i++) {
			if (arr[i] == null || arr[i].length != h)
				throw new RuntimeException("ragged array");
		}

		_map = new int[arr.length][h];
		for (int i = 0; i < arr.length; i++) {
			System.arraycopy(arr[i], 0, _map[i], 0, h);
		}
	}
	/**
	 * Computes a deep copy of the underline 2D matrix.
	 * @return a deep copy of the underline matrix.
	 */
	@Override
	public int[][] getMap() {
		int w = getWidth(), h = getHeight();
		int[][] copy = new int[w][h];
		for (int x = 0; x < w; x++) System.arraycopy(_map[x], 0, copy[x], 0, h);
		return copy;
	}
	/**@return the width of this 2D map (first coordinate).*/
	@Override
	public int getWidth() {return _map.length;}
	/**@return the height of this 2D map (second coordinate).*/
	@Override
	public int getHeight() {return _map[0].length;}
	/**
	 * @param x the x coordinate
	 * @param y the y coordinate
	 * @return the [x][y] (int) value of the map[x][y].
	 */
	@Override
	public int getPixel(int x, int y) { return _map[x][y]; }
	/**
	 * @param p the x,y coordinate
	 * @return the [p.x][p.y] (int) value of the map.
	 */
	@Override
	public int getPixel(Pixel2D p) {
		return this.getPixel(p.getX(),p.getY());
	}
	/**
	 * Set the [x][y] coordinate of the map to v.
	 * @param x the x coordinate
	 * @param y the y coordinate
	 * @param v the value that the entry at the coordinate [x][y] is set to.
	 */
	@Override
	public void setPixel(int x, int y, int v) { _map[x][y] = v;}
	/**
	 * Set the [x][y] coordinate of the map to v.
	 * @param p the coordinate in the map.
	 * @param v the value that the entry at the coordinate [p.x][p.y] is set to.
	 */
	@Override
	public void setPixel(Pixel2D p, int v) {
		setPixel(p.getX(), p.getY(), v);
	}
	/**
	 * @param p the 2D coordinate.
	 * @return true iff p is with in this map.
	 */
	@Override
	public boolean isInside(Pixel2D p){
		if (p == null) return false;
		int x = p.getX(), y = p.getY();
		return x >= 0 && y >= 0 && x < getWidth() && y < getHeight();
	}
	/** @return true iff this map should be addressed as a cyclic one.*/
	@Override
	public boolean isCyclic() {
		return _cyclicFlag;
	}
	/**
	 * Set the cyclic flag of this map
	 * @param cy the value of the cyclic flag.
	 */
	@Override
	public void setCyclic(boolean cy){ _cyclicFlag = cy; }
								///////////////// Internal functions //////////////////
	private int[][] neighbors4(int x, int y) {
		return new int[][]{
				{x, y - 1}, // UP
				{x - 1, y}, // LEFT
				{x, y + 1}, // DOWN
				{x + 1, y}  // RIGHT
		};
	}
	private int wrapX(int x) {
		int w = getWidth();
		x %= w;
		if (x < 0) x += w;
		return x;
	}
	private int wrapY(int y) {
		int h = getHeight();
		y %= h;
		if (y < 0) y += h;
		return y;
	}
								///////////////// Algorithms //////////////////
	/**
	 * Fill the connected component of p in the new color (new_v).
	 * Note: the connected component of p are all the pixels in the map with the same "color" of map[p] which are connected to p.
	 * Note: two pixels (p1,p2) are connected if there is a path between p1 and p2 with the same color (of p1 and p2).
	 * @param xy the pixel to start from.
	 * @param new_v - the new "color" to be filled in p's connected component.
	 * @return the number of "filled" pixels.
	 */
	@Override
	public int fill(Pixel2D xy, int new_v) {
		if (xy == null) throw new RuntimeException("start==null");
		if (!isInside(xy)) return 0;

		int old = getPixel(xy);
		if (old == new_v) return 0;

		int w = getWidth(), h = getHeight();
		boolean[][] vis = new boolean[w][h];
		ArrayDeque<Index2D> q = new ArrayDeque<>();

		q.add(new Index2D(xy));
		vis[xy.getX()][xy.getY()] = true;

		int count = 0;
		while (!q.isEmpty()) {
			Index2D p = q.removeFirst();
			int x = p.getX(), y = p.getY();

			if (getPixel(x, y) != old) continue;
			setPixel(x, y, new_v);
			count++;

			for (int[] nb : neighbors4(x, y)) {
				int nx = nb[0], ny = nb[1];

				if (isCyclic()) { nx = wrapX(nx); ny = wrapY(ny); }
				if (nx < 0 || ny < 0 || nx >= w || ny >= h) continue;

				if (!vis[nx][ny]) {
					vis[nx][ny] = true;
					q.add(new Index2D(nx, ny));
				}
			}
		}
		return count;
	}
	/**
	 * @param p1 first coordinate (start point).
	 * @param p2 second coordinate (end point).
	 * @param obsColor the color which is addressed as an obstacle.
	 * @return the shortest path as an array of consecutive pixels, if none - returns null.
	 */
	@Override
	public Pixel2D[] shortestPath(Pixel2D p1, Pixel2D p2, int obsColor) {
		if (p1 == null || p2 == null) throw new RuntimeException("null arg");
		if (!isInside(p1) || !isInside(p2)) return null;
		if (getPixel(p1) == obsColor || getPixel(p2) == obsColor) return null;

		int w = getWidth(), h = getHeight();
		boolean[][] vis = new boolean[w][h];
		Index2D[][] parent = new Index2D[w][h];

		ArrayDeque<Index2D> q = new ArrayDeque<>();
		q.add(new Index2D(p1));
		vis[p1.getX()][p1.getY()] = true;

		while (!q.isEmpty()) {
			Index2D p = q.removeFirst();
			int x = p.getX(), y = p.getY();

			if (x == p2.getX() && y == p2.getY()) break;

			for (int[] nb : neighbors4(x, y)) {
				int nx = nb[0], ny = nb[1];

				if (isCyclic()) { nx = wrapX(nx); ny = wrapY(ny); }
				if (nx < 0 || ny < 0 || nx >= w || ny >= h) continue;

				if (vis[nx][ny]) continue;
				if (getPixel(nx, ny) == obsColor) continue;

				vis[nx][ny] = true;
				parent[nx][ny] = new Index2D(x, y);
				q.add(new Index2D(nx, ny));
			}
		}

		if (!vis[p2.getX()][p2.getY()]) return null;

		ArrayList<Pixel2D> rev = new ArrayList<>();
		Index2D cur = new Index2D(p2);
		while (cur != null) {
			rev.add(cur);
			if (cur.equals(p1)) break;
			cur = parent[cur.getX()][cur.getY()];
		}

		Pixel2D[] path = new Pixel2D[rev.size()];
		for (int i = 0; i < rev.size(); i++) {
			path[i] = rev.get(rev.size() - 1 - i);
		}
		return path;
	}
	/**
	 * Compute a new map (with the same dimension as this map) with the
	 * shortest path distance (obstacle avoiding) from the start point.
	 * None accessible entries should be marked -1.
	 * @param start the source (starting) point
	 * @param obsColor the color representing obstacles
	 * @return a new map with all the shortest path distances from the starting point to each entry in this map.
	 */
	@Override
	public Map2D allDistance(Pixel2D start, int obsColor) {
		if (start == null) throw new RuntimeException("start==null");

		int w = getWidth(), h = getHeight();
		Map dist = new Map(w, h, -1);
		dist.setCyclic(this.isCyclic());

		if (!isInside(start)) return dist;
		if (getPixel(start) == obsColor) return dist;

		ArrayDeque<Index2D> q = new ArrayDeque<>();
		dist.setPixel(start, 0);
		q.add(new Index2D(start));

		while (!q.isEmpty()) {
			Index2D p = q.removeFirst();
			int x = p.getX(), y = p.getY();
			int d = dist.getPixel(x, y);

			for (int[] nb : neighbors4(x, y)) {
				int nx = nb[0], ny = nb[1];

				if (isCyclic()) { nx = wrapX(nx); ny = wrapY(ny); }
				if (nx < 0 || ny < 0 || nx >= w || ny >= h) continue;

				if (getPixel(nx, ny) == obsColor) continue;
				if (dist.getPixel(nx, ny) != -1) continue;

				dist.setPixel(nx, ny, d + 1);
				q.add(new Index2D(nx, ny));
			}
		}
		return dist;
	}
}
