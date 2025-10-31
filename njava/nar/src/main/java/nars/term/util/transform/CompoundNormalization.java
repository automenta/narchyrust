//package nars.term.util.transform;
//
//import nars.$;
//import nars.subterm.Subterms;
//import nars.term.Compound;
//import nars.term.Neg;
//import nars.term.Term;
//import nars.term.atom.Int;
//import nars.term.util.Image;
//import nars.term.util.TermTransformException;
//
///** procedure for Compound target Normalization */
//public final class CompoundNormalization extends VariableNormalization {
//
//    private final Compound root;
//    private final boolean imgPossible;
//
//    /** TODO check */
//    private static final int MIN_IMAGE_VOL = Image.imageExt($.inh($.pFast(Int.ONE), Int.the(2)), Int.ONE).volume();
//
//    public CompoundNormalization(Compound x, byte varOffset) {
//        super(x.vars() /* estimate */, varOffset);
//        this.root = x;
//        Subterms xx = x.subtermsDirect();
//        this.imgPossible = xx.volume() >= MIN_IMAGE_VOL && xx.hasAll(Image.ImageBits);
//    }
//
//    @Override public boolean preFilter(Compound x) {
//        return super.preFilter(x) || (imgPossible && x.hasAll(Image.ImageBits));
//    }
//
//    @Override
//    public Term applyCompound(Compound x) {
//        if (x instanceof Neg) return applyNeg((Neg)x);
//
//        /* if x is not the root target (ie. a subterm) */
//        boolean hasImg = imgPossible && x.hasAll(Image.ImageBits);
//        if (hasImg && x!=root && x.INH()) {
//
//            Term _y = Image._imageNormalize(x);
//
//            if (!(_y instanceof Compound))
//               throw new TermTransformException("image normalization error", x, _y);
//
//            Compound y = (Compound) _y;
//            if (x!=y)
//                hasImg = (x = y).hasAll(Image.ImageBits); //check if image bits remain
//        }
//        return hasImg || x.hasVars() ? super.applyCompound(x) : x;
//    }
//
//
//
//}
