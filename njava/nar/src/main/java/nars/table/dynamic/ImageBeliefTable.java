package nars.table.dynamic;

import nars.Term;
import nars.term.util.Image;

public class ImageBeliefTable extends ProxyBeliefTable {

    public ImageBeliefTable(Term image, boolean beliefOrGoal) {
        super(image, beliefOrGoal);

        assert(image.INH()): "ImageBeliefTable " + image + " -> " + resolve(image);
    }

    @Override
    public Term resolve(Term external) {
        return Image.imageNormalize(external);
    }

}