package nars.experiment.pacman.entities;

import nars.experiment.pacman.maze.Maze;
import nars.experiment.pacman.maze.Maze.Direction;

abstract class Entity {

    public double x;
    public double y;
    public Direction dir;
    double speed;
    double lead;
    Maze maze;

    Entity(Maze maze, int x, int y) {

        this.x = x;
        this.y = y;
        speed = 0.1;
        lead = 0.1;
        dir = Direction.right;
        this.maze = maze;

    }

    public void update() {

        move(dir, speed);

        if (dir == Direction.left && x <= -1) x = maze.width;
        if (dir == Direction.right && x >= maze.width) x = -1;
        if (dir == Direction.up && y <= -1) y = maze.height;
        if (dir == Direction.down && y >= maze.height) y = -1;

        if (this.dir == Direction.up || this.dir == Direction.down) {

            if (Math.round(x) > x) x += 0.1;

            if (Math.round(x) < x) x -= 0.1;

        }

        if (this.dir == Direction.left || this.dir == Direction.right) {

            if (Math.round(y) > y) y += 0.1;

            if (Math.round(y) < y) y -= 0.1;

        }

    }

    public void move(Direction dir, double dist) {

        if (dir == null) return;

        byte[][] t = maze.tiles;
        int yp = (int) Math.floor(y + lead);
        int yn = (int) Math.ceil(y - lead);
        switch (dir) {
            case up -> {
                if (y < 1 - dist) {

                    y -= dist;

                } else if (Maze.isWall(t[(int) Math.floor(x + lead)][(int) Math.ceil(y) - 1], Direction.up) ||
                        Maze.isWall(t[(int) Math.ceil(x - lead)][(int) Math.ceil(y) - 1], Direction.up))
                    y = Math.ceil(y);
                else
                    y -= dist;
            }
            case down -> {
                if (y > maze.height - 2 + dist) {

                    y += dist;

                } else if (Maze.isWall(t[(int) Math.floor(x + lead)][(int) Math.floor(y) + 1], Direction.down) ||
                        Maze.isWall(t[(int) Math.ceil(x - lead)][(int) Math.floor(y) + 1], Direction.down))
                    y = Math.floor(y);
                else y += dist;
            }
            case left -> {
                if (x < 1 - dist) {

                    x -= dist;

                } else {
                    int xn = (int) Math.ceil(x) - 1;
                    if (Maze.isWall(t[xn][yp], Direction.left) ||
                            Maze.isWall(t[xn][yn], Direction.left))
                        x = Math.ceil(x);
                    else x -= dist;
                }
            }
            case right -> {
                if (x > maze.width - 2 + dist) {

                    x += dist;

                } else {
                    int xp = (int) Math.floor(x) + 1;
                    if (Maze.isWall(t[xp][yp], Direction.right) ||
                            Maze.isWall(t[xp][yn], Direction.right))
                        x = Math.floor(x);
                    else x += dist;
                }
            }
        }

    }

    public boolean walled(Direction dir) {

        try {

            byte[][] t = maze.tiles;
            long xi = Math.round(x);
            switch (dir) {
                case left -> {
                    if (Maze.isWall(t[(int) xi - 1][(int) Math.floor(y + lead)], dir)) return true;
                    if (Maze.isWall(t[(int) xi - 1][(int) Math.ceil(y - lead)], dir)) return true;
                }
                case right -> {
                    if (Maze.isWall(t[(int) xi + 1][(int) Math.floor(y + lead)], dir)) return true;
                    if (Maze.isWall(t[(int) xi + 1][(int) Math.ceil(y - lead)], dir)) return true;
                }
                case up -> {
                    if (Maze.isWall(t[(int) Math.floor(x + lead)][(int) Math.round(y) - 1], dir)) return true;
                    if (Maze.isWall(t[(int) Math.ceil(x - lead)][(int) Math.round(y) - 1], dir)) return true;
                }
                case down -> {
                    if (Maze.isWall(t[(int) Math.floor(x + lead)][(int) Math.round(y) + 1], dir)) return true;
                    if (Maze.isWall(t[(int) Math.ceil(x - lead)][(int) Math.round(y) + 1], dir)) return true;
                }
            }

        } catch (ArrayIndexOutOfBoundsException e) {

            return true;

        }

        return false;

    }

}