package nars.nal.multistep;

import nars.*;
import nars.term.atom.Atomic;
import nars.term.atom.Int;

import java.util.Set;

import static nars.Op.*;

/** from: https://github.com/opennars/OpenNARS-for-Applications/blob/master/misc/Python/gridplan.py */
public class GridPlanTest {

    static final int sx =
        //6;
        4;
        //3;
        //2; //EASY

    static final int sy = sx;
    public static final Atomic LEFT = $.atomic("left");
    public static final Atomic RIGHT = $.atomic("right");
    public static final Atomic UP = $.atomic("up");
    public static final Atomic DOWN = $.atomic("down");

    final Set<Term> unreachables = java.util.Set.of(
        state(3,3), state(3,4), state(3,5), state(5,3)
        // # (6,7), (6,8), (6,9)
    );

    private final NAR nar;

    /** position coord */
    int px = 0, py = 0;

    /** goal coord */
    int gx = sx-1, gy = sy-1;


    Term state(int x, int y) {
        return state(Int.i(x), Int.i(y));
    }

    Term state(Term x, Term y) {
        return $.p(x, y); //PROD
        //return $.the(x + " " + y); //ATOM
    }

    /** "<(" + state(pre) + " &/ " + op + ") =/> " + state(cons) + ">." */
    Term transition(Term pre, String op, Term cons) {
        return IMPL.the(CONJ.the(pre, $.atomic(op)), +1, cons);
    }

    public void knowledge() {
        for (int x = 0; x < sx; x++) {
            for (int y = 0; y < sy; y++) {
                if (unreachable(x, y))
                    continue;
                var at = state(x, y);
                var left = state(x-1, y);
                var right = state(x+1, y);
                var up = state(x, y+1);
                var down = state(x, y-1);
                if (x > 0)
                    nar.believe(transition(left, "right", at));
                if (x < sx-1)
                    nar.believe(transition(right, "left", at));
                if (y > 0)
                    nar.believe(transition(down, "up", at));
                if (y < sy-1)
                    nar.believe(transition(up, "down", at));
            }
        }

        long now = nar.time();
        nar.want(state(gx, gy), 1, 0.9f,
                ETERNAL,ETERNAL
                //now, now /* ETERNAL */
        );

//        try {
//            nar.believe("--(left<->right)");
//            nar.believe("--(up<->down)");
//        } catch (Narsese.NarseseException e) {
//            throw new RuntimeException(e);
//        }
    }

    private boolean unreachable(int x, int y) {
        return unreachables.contains(state(x, y));
    }

    public GridPlanTest(NAR nar) {
        this.nar = nar;

        nar.log();

//        nar.main().onTask(z -> {
//            if (!((NALTask)z).isInput())
//                System.out.println(z);
//        }, Op.GOAL);

        knowledge();

        boolean moved = true;

        while (true) {
            long now = nar.time();

//            if (now % (sx*sy) == 1)
//                knowledge();
            if (now/10 % (sx*sy) == 1) {
            //if (moved) {
                for (int x = 0; x < sx; x++)
                    for (int y = 0; y < sy; y++)
                        nar.believe(state(x,y).negIf(px!=x || py!=y), now);

                //nar.believe(state(px, py), now);
                moved = false;
            }

            nar.run(1);

            if (act())
                moved = true;

            if (px == gx && py == gy) {
                System.out.println("WIN");
                break;
            }

            /*

            print("\033[1;1H\033[2J") #clear screen
            #prepare grid:
            curfield = copy.deepcopy(field)
            curfield[position[0]][position[1]] = "X "
            curfield[goal[0]][goal[1]] = "G "
            for (x, y) in unreachables:
                curfield[x][y] = "##"
            #draw grid:
            for x in range(SX+2):
                print("##", end="")
            print()
            for x in range(SY):
                print("##", end="")
                for y in curfield[x]:
                    print(y, end="")
                print("##")
            for x in range(SX+2):
                print("##", end="")
            print()
            sys.stdout.flush()
            time.sleep(0.1)

             */
        }
    }

    public boolean act() {
        int px0 = px, py0 = py;

        double l = desire(LEFT), r = desire(RIGHT), u = desire(UP), d = desire(DOWN);

        Term action = null;
        if (px > 0 && stronger(l, r)) {
            px--;
            action = LEFT;
        } else if (px < sx-1 && stronger(r, l)) {
            px++;
            action = RIGHT;
        }

        if (py < sy-1 && stronger(u, d)) {
            py++;
            action = UP;
        } else if (py > 0 && stronger(d, u)) {
            py--;
            action = DOWN;
        }

        if (action!=null) {
            if (unreachable(px, py)) {
                px = px0;
                py = py0;
                System.out.println("can't " + action);
            } else {
                nar.believe(action, nar.time(), nar.time());
                System.out.println(action + " -> " + state(px, py));
                return true;
            }
        }
        return false;
    }

    private boolean stronger(double a, double b) {
        //return a - b > 0.5 * min(a,b);
        return a > b;
    }

    private double desire(Term x) {
        long now = nar.time();
        float dur = nar.dur();
        Truth t = nar.goalTruth(x, now, now, dur);
        return t == null ? 0 : t.expectation();
    }


    public static void main(String[] args) {
        NAR n = NARS.tmp();
        n.time.dur(64);
        n.complexMax.set(9);

        new GridPlanTest(n);
    }


}
