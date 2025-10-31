package nars.experiment.pacman.entities;

import nars.experiment.pacman.maze.Maze;
import nars.experiment.pacman.maze.Maze.Direction;

public class Player extends Entity {

    public static final int MAX_POWER = 500;
    public int mouthAngle = 10;
    int mouthSpeed = 2;
    static final int MOUTH_WIDTH = 40;
    public int power;


    public Player(Maze maze, int x, int y, float speed) {

        super(maze, x, y);

        this.speed = speed;
        this.lead = 0.4;

        power = 0;

    }

    @Override
    public void update() {

        super.update();

        power = Math.max(0, power - 1);

        try {

            int X = (int) (x / 2);
            int Y = (int) (y / 2);
            if (maze.dots[X][Y]) {

                maze.dots[X][Y] = false;
                maze.dotCount--;

                if (maze.isBigFood(2 * X + 1, 2 * Y + 1))
                    power = MAX_POWER;
            }

        } catch (ArrayIndexOutOfBoundsException e) {

        }

        mouthAngle += mouthSpeed;
        if (mouthAngle >= MOUTH_WIDTH || mouthAngle <= 0)
            mouthSpeed = -mouthSpeed;

    }

    public void turn(Direction d) {

        if (!walled(d)) {

            this.dir = d;

        }

    }

    public boolean die() {


        this.x = maze.playerStart().x;
        this.y = maze.playerStart().y;
        this.mouthAngle = 5;
        this.dir = Direction.right;


        return false;

    }

    public boolean deadFinally() {

        if (mouthAngle >= 180) {
            return true;
        } else {
            mouthAngle = Math.min(180, mouthAngle + 5);
            return false;
        }


    }

}