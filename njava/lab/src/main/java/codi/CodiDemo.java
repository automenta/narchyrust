package codi;

import com.jogamp.opengl.GL2;
import jcog.exe.Loop;
import jcog.nndepr.spiking0.ca.CodiCA;
import spacegraph.SpaceGraph;
import spacegraph.space2d.ReSurface;
import spacegraph.space2d.container.PaintSurface;
import spacegraph.video.Draw;

import static jcog.nndepr.spiking0.ca.CodiCA.*;


public class CodiDemo {

    public static class CodiSurface extends PaintSurface {

        private final CodiCA c;


        public CodiSurface(CodiCA c) {
            this.c = c;
        }

        @Override
        protected void paint(GL2 gl, ReSurface reSurface) {
            float tw = w()/c.sizeX;
            float th = h()/c.sizeY;


            for (int x = 0; x < c.sizeX; x++) {
                for (int y = 0; y < c.sizeY; y++) {
                    for (int z = 0; z < c.sizeZ; z++) {
                        CodiCA.CodiCell ca = c.cell[x][y][z];
                        int type = ca.Type;
                        if (type == 0)
                            continue;

                        float px = x * tw;
                        float py = y * th;
                        float pz = z * tw;


                        float r, g, b;
                        switch (type) {
                            case NEURON -> {
                                r = 0.75f;
                                g = 0.75f;
                                b = 0.75f;
                            }
                            case AXON -> {
                                r = 0.75f;
                                g = 0;
                                b = 0;
                            }
                            case DEND -> {
                                r = 0;
                                g = 0.75f;
                                b = 0;
                            }
                            default -> throw new UnsupportedOperationException();
                        }

                        gl.glColor4f(r, g, b, 0.5f);
                        Draw.rect(px, py, tw, th, gl);

                        float activation = ca.Activ;
                        if (activation != 0) {
                            float a = 0.5f + 0.5f * Math.abs(activation) / (FIRING_THRESHOLD+1);

                            if (activation > 0)
                                gl.glColor4f(1f, 0.25f + 0.5f * a, 0f, a);
                            else
                                gl.glColor4f(0f, 0.25f + 0.5f * a, 1f, a);
                            Draw.rect(px + tw / 4, py + th / 4, tw / 2, th / 2, gl);
                        }


                    }
                }
            }
        }
    }

    public static void main(String[] args) {
        CodiCA c = new CodiCA(128, 128, 1);

        Loop.of(c::next).fps(30);

        SpaceGraph.window(new CodiSurface(c), 1000, 1000);
    }
}
