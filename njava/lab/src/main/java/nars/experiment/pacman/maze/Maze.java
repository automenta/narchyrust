package nars.experiment.pacman.maze;

import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

public class Maze {

    public Fruit fruit = Fruit.none;
    public int width;
    public int height;
    public byte[][] tiles;
    public boolean[][] dots;
    public int dotCount;
    private int[][] bigDots;
    private GenCursor[] cursors;

    private Maze(int width, int height) {

        this.width = width;
        this.height = height;

        tiles = new byte[width][height];
        dots = new boolean[width / 2][height / 2];
        dotCount = (width / 2) * (height / 2);
        bigDots = new int[][]{{1, 1}, {width - 2, 1}, {width - 2, height - 2}, {1, height - 2}};

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {

                tiles[x][y] = (byte) ((x % 2) == 1 && (y % 2) == 1 ? 1 : 2);

                if ((x * y) % 2 == 1)
                    dots[x / 2][y / 2] = true;

            }
        }

    }

    public static Maze create(int width, int height) {

        width |= 1;
        height |= 3;
        width ^= width & 2;

        Maze half = new Maze(width / 2 + 1, height);
        half.generate();
        Maze full = half.doubleUp();

        Dimension ghostCage = new Dimension(5, 3);
        int ghw = ghostCage.width;
        int ghh = ghostCage.height;
        Point offset = new Point(width / 2 - ghw / 2, height / 2 - ghh / 2);

        for (int ix = 0; ix < ghw; ix++) {
            for (int iy = 0; iy < ghh; iy++) {

                if (full.dots[(offset.x + ix) / 2][(offset.y + iy) / 2]) {
                    full.dots[(offset.x + ix) / 2][(offset.y + iy) / 2] = false;
                    full.dotCount--;
                }

                if (iy == 1 && ix > 2 && ix < ghw - 3) {
                    full.tiles[ix + offset.x][1 + offset.y] = 3;
                    continue;
                }

                if (ix == 0 || iy == 0 || ix == ghw - 1 || iy == ghh - 1) {
                    full.tiles[ix + offset.x][iy + offset.y] = 1;
                    continue;
                }

                if (ix == 1 || iy == 1 || ix == ghw - 2 || iy == ghh - 2) {
                    full.tiles[ix + offset.x][iy + offset.y] = 2;
                    continue;
                }

                full.tiles[ix + offset.x][iy + offset.y] = 0;

            }
        }

        return full;

    }

    public static boolean isWall(byte b) {
        return b == 3 || b == 2;
    }

    public static boolean isWall(byte b, Direction d) {
        return b == 3 && d != Direction.up || b == 2;
    }

    public Point playerStart() {
        return new Point(width / 2, height / 2 + 2);
    }

    private void generate(Rectangle area) {

        area.x ^= area.x & 1;
        area.y ^= area.y & 1;
        area.width |= 1;
        area.height |= 1;

        cursors = new GenCursor[]{new GenCursor(this, 1, 1), new GenCursor(this, 1, 1)};

        for (GenCursor c : cursors) {
            c.area = area;
            c.x = area.x + 1;
            c.y = area.y + 1;
        }

        int finished = 0;
        while (finished < this.cursors.length) {

            for (GenCursor c : this.cursors) {

                c.advance();

                if (c.complTrig) finished++;

            }

        }

        fixDots(area);

        for (int x = 0; x < width; x++) {

            if (tiles[x][0] == 1) {

                tiles[x][height - 1] = 1;

            }

            if (tiles[x][height - 1] == 1) {

                tiles[x][0] = 1;

            }

        }

    }

    private Maze doubleUp() {

        Maze other = new Maze(2 * width - 1, height);

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                other.tiles[x][y] = tiles[x][y];
                other.tiles[width + x - 1][y] = tiles[width - x - 1][y];
            }
        }

        other.connectVert(new Point(width, 0), height);

        return other;

    }

    private void connectVert(Point p, int height) {

        p.x ^= p.x & 1;
        p.y |= 1;
        height ^= height & 1;

        int walls = 0;

        for (int i = 0; i <= height; i += 2) {

            try {

                if (!isWall(tiles[p.x - 1][p.y + i]) && !isWall(tiles[p.x + 1][p.y + i])) {

                    if (!isClosed(p.x - 1, p.y + i) && !isClosed(p.x + 1, p.y + i)) {

                        if (Math.random() >= 1.5 * (height - i + 3 * walls) / (2 * height + walls)) {

                            walls++;

                            tiles[p.x][p.y + i] = 1;
                            i += 4;

                        }

                    }

                }


            } catch (ArrayIndexOutOfBoundsException e) {

                continue;

            }

        }

    }

    private void fixDots(Rectangle area) {

        area.x ^= area.x & 1;
        area.y ^= area.y & 1;

        for (int x = area.x; x < area.x + area.width; x++) {
            for (int y = area.y; y < area.y + area.height; y++) {

                if (isDot(x, y)) {

                    boolean[] corners = {false, false, false, false};

                    if (!isWall(tiles[x - 2][y - 1]) || !isWall(tiles[x - 1][y - 2])) corners[0] = true;
                    if (!isWall(tiles[x + 2][y - 1]) || !isWall(tiles[x + 1][y - 2])) corners[1] = true;
                    if (!isWall(tiles[x - 2][y + 1]) || !isWall(tiles[x - 1][y + 2])) corners[2] = true;
                    if (!isWall(tiles[x + 2][y + 1]) || !isWall(tiles[x + 1][y + 2])) corners[3] = true;

                    if (corners[0] && corners[1]) tiles[x][y - 1] = 2;
                    else if (corners[1] && corners[3]) tiles[x + 1][y] = 2;
                    else if (corners[2] && corners[3]) tiles[x][y + 1] = 2;
                    else if (corners[0] && corners[2]) tiles[x - 1][y] = 2;

                }

            }
        }

    }

    private boolean isDot(int x, int y) {

        if (x % 2 == 1) return false;
        if (y % 2 == 1) return false;

        if (x <= 0) return false;
        if (y <= 0) return false;
        if (x >= width - 1) return false;
        if (y >= height - 1) return false;

        if (!isWall(tiles[x][y])) return false;

        if (isWall(tiles[x - 1][y])) return false;
        if (isWall(tiles[x + 1][y])) return false;
        if (isWall(tiles[x][y - 1])) return false;
        return !isWall(tiles[x][y + 1]);

    }

    private void generate() {

        generate(new Rectangle(0, 0, width, height));

    }

    Direction[] getUngeneratedDirections(Point p, Rectangle area) {

        ArrayList<Direction> openDirs = new ArrayList<>();

        if (p.x > area.x + 2)
            if (isClosed(p.x - 2, p.y)) openDirs.add(Direction.left);
        if (p.y > area.y + 2)
            if (isClosed(p.x, p.y - 2)) openDirs.add(Direction.up);
        if (p.y < area.y + area.height - 2)
            if (isClosed(p.x, p.y + 2)) openDirs.add(Direction.down);
        if (p.x < area.x + area.width - 2)
            if (isClosed(p.x + 2, p.y)) openDirs.add(Direction.right);

        Direction[] returnedDirs = new Direction[openDirs.size()];
        openDirs.toArray(returnedDirs);
        return returnedDirs;

    }

    private boolean isClosed(int x, int y) {

        return isClosed(new Point(x, y));

    }

    private boolean isClosed(Point p) {

        return isWall(p.x - 1, p.y) &&
                isWall(p.x + 1, p.y) &&
                isWall(p.x, p.y - 1) &&
                isWall(p.x, p.y + 1);

    }

    boolean isWall(int x, int y) {

        if (x <= 0) return true;
        if (x >= width - 1) return true;
        if (y <= 0) return true;
        if (y >= height - 1) return true;

        return isWall(tiles[x][y]);

    }

    public boolean isBigFood(int x, int y) {

        int[] check = {x, y};

        return Arrays.stream(this.bigDots).anyMatch(place -> check[0] == place[0] && check[1] == place[1]);

    }

    public enum Direction {

        up, left, down, right;

        public Direction opposite() {

            return switch (this) {
                case up -> down;
                case down -> up;
                case left -> right;
                case right -> left;
            };

        }

    }

    public enum Fruit {
        none, red, yellow, blue
    }

    public static class GenCursor {

        public int x, y;
        Maze maze;
        public Rectangle area;
        ArrayList<Direction> path;
        final Random rand = new Random();
        boolean trigger;

        public boolean complete;
        public boolean complTrig;

        public GenCursor(Maze m, int x, int y) {

            this.maze = m;
            this.x = x;
            this.y = y;

            this.area = new Rectangle(0, 0, maze.width, maze.height);

            path = new ArrayList<>();

        }

        public boolean advance() {

            Direction[] openDirs = maze.getUngeneratedDirections(new Point(this.x, this.y), area);

            if (openDirs.length > 0) {

                trigger = true;

                Direction move = openDirs[rand.nextInt(openDirs.length)];
                maze.tiles[motion(move, 1).x][motion(move, 1).y] = 1;
                this.x = motion(move, 2).x;
                this.y = motion(move, 2).y;
                path.add(move);

            } else {

                if (trigger) {

                    Direction[] dirs = getPunchDirections();

                    for (int x = 0; x < dirs.length; x++)
                        maze.tiles[motion(dirs[x], 1).x][motion(dirs[x], 1).y] = 1;

                    trigger = false;

                }

                if (path.size() > 0) {

                    Direction move = path.get(path.size() - 1).opposite();
                    this.x = motion(move, 2).x;
                    this.y = motion(move, 2).y;

                    path.remove(path.size() - 1);

                } else {

                    complTrig = !complete;

                    complete = true;
                    return false;

                }

            }

            return true;

        }

        public Point motion(Direction dir, int dist) {

            return switch (dir) {
                case up -> new Point(this.x, this.y - dist);
                case down -> new Point(this.x, this.y + dist);
                case left -> new Point(this.x - dist, this.y);
                case right -> new Point(this.x + dist, this.y);
                default -> null;
            };

        }

        public Direction[] getPunchDirections() {

            Direction past = null;
            if (path.size() > 2)
                past = path.get(path.size() - 2);

            ArrayList<Direction> openDirs = new ArrayList<>();
            if (past != Direction.right)
                if (maze.isWall(x - 1, y) && x > area.x)
                    openDirs.add(Direction.left);
            if (past != Direction.left)
                if (maze.isWall(x + 1, y) && x < area.x + area.width - 1)
                    openDirs.add(Direction.right);
            if (past != Direction.down)
                if (maze.isWall(x, y - 1) && y > area.y + 2)
                    openDirs.add(Direction.up);
                else if (maze.isWall(x, y - 1) && y > area.y && x > area.x + 1 && x < area.width - 2)
                    openDirs.add(Direction.up);
            if (past != Direction.up)
                if (maze.isWall(x, y + 1) && y < area.y + area.height - 2)
                    openDirs.add(Direction.down);
                else if (maze.isWall(x, y + 1) && y < area.y + area.height - 1 && x > area.x + 1 && x < area.width - 2)
                    openDirs.add(Direction.down);

            if (openDirs.size() > 1) {
                if (path.size() > 1)
                    openDirs.remove(path.get(path.size() - 1));
            }

            Direction[] returnedDirs = new Direction[openDirs.size()];
            openDirs.toArray(returnedDirs);
            return returnedDirs;

        }

    }
}