package nars.func.java;

import jcog.Is;
import jcog.Log;
import jcog.Research;
import jcog.Util;
import jcog.data.list.Lst;
import jcog.data.map.ConcurrentFastIteratingHashSet;
import jcog.data.map.CustomConcurrentHashMap;
import jcog.memoize.CaffeineMemoize;
import jcog.memoize.Memoize;
import jcog.signal.FloatRange;
import jcog.util.ArrayUtil;
import nars.*;
import nars.subterm.Subterms;
import nars.term.Compound;
import nars.term.Functor;
import nars.term.Neg;
import nars.term.ProxyCompound;
import nars.term.atom.Atom;
import nars.term.atom.Atomic;
import nars.term.var.Variable;
import nars.time.part.DurLoop;
import nars.truth.PreciseTruth;
import nars.util.NARPart;
import nars.util.OpExec;
import nars.util.TaskChannel;
import nars.util.Timed;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.dynamic.scaffold.TypeValidation;
import net.bytebuddy.implementation.InvocationHandlerAdapter;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.implementation.bind.annotation.*;
import net.bytebuddy.matcher.ElementMatchers;
import org.eclipse.collections.api.tuple.Pair;
import org.jetbrains.annotations.Nullable;
import org.junit.platform.commons.util.Preconditions;
import org.slf4j.Logger;

import java.lang.invoke.ConstantCallSite;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;

import static java.lang.Math.round;
import static jcog.data.map.CustomConcurrentHashMap.*;
import static nars.NAL.truth.FREQ_EPSILON;
import static nars.Op.*;
import static org.eclipse.collections.impl.tuple.Tuples.pair;


/**
 * Opjects - Operable Objects
 * Transparent JVM Metaprogramming Interface for Non-Axiomatic Logic Reasoners
 * <p>
 * Generates dynamic proxy classes for any POJO that intercepts specific
 * methods and generates reasoner events which can be
 * stored, input to one or more reasoners, etc..
 * <p>
 * An Opjects instance manages a set of ("opject") proxy instances and their activity,
 * whether either by a user (ex: while training) or the NAR itself (ex: after trained).
 * <p>
 * Invoke -  invoked internally, deliberately as a result of NAR activity.
 * <p>
 * Evoke -  externally caused execution (by user or other computer process).
 * in a sense the NAR is puppeted by these actions, because it perceives them,
 * to some degree, as causing them itself.  however, from a user's perspective,
 * we clearly distinguish between Evoked and Invoked procedures.
 * <p>
 * Evocation trains the NAR how to invoke.  During on-line use, it provides
 * asynchronously triggered feedback which can inform and trigger the NAR
 * for what it has learned, or any other task.
 * <p>
 * TODO option to record stack traces
 * TODO the results need to be buffered each cycle to avoid inputting multiple boolean-returning tasks that contradict each other
 */
@Research
@Is({"Metaprogramming", "Reinforcement_learning"})
public class Opjects extends NARPart {

    static final Logger logger = Log.log(Opjects.class);
    static final ClassLoadingStrategy classLoadingStrategy =
            ClassLoadingStrategy.Default.WRAPPER;

    private final ByteBuddy bb = new ByteBuddy();

    /** evocation execution threshold */
    public final FloatRange exeThresh = new FloatRange(0.5f, 0.5f, 1f);
    private final MyDefaultTermizer term = new MyDefaultTermizer();


    /**
     * determines evidence weighting for reporting specific feedback values
     */
    float beliefEviFactor = 1f;
    float invokeEviFactor = beliefEviFactor;
    float beliefFreq = 1f;
    float invokeFreq = 1f;


//    /**
//     * determines evidence weighting for reporting assumed feedback assumptions
//     */
//    float doubtEviFactor = 0.75f;
//    float uninvokeEviFactor = 1;
//    float doubtFreq = 0.5f;
//    float uninvokeFreq = 1f - invokeFreq;

    public final FloatRange pri = new FloatRange(1f, 0f, 1f);

    /** belief pri (factor) TODO use PriNode */
    public final FloatRange beliefPri = new FloatRange(0.5f, 0, 1);

    /**
     * cached; updated at most each duration
     */
    private double beliefEvi = 0;
    private float _beliefPri = 0;
    private double invokeEvi;


    public final Set<String> methodExclusions = new CopyOnWriteArraySet<>(java.util.Set.of(
            "hashCode",
            "notify",
            "notifyAll",
            "wait",
            "finalize",
            "stream",
            "iterator",
            "getHandler",
            "setHandler",
            "toString",
            "equals"
    ));

    final Map<Class, Class> proxyCache = new CustomConcurrentHashMap(STRONG, EQUALS, SOFT, IDENTITY, 64);

    final Map<Class, Boolean> clCache = new CustomConcurrentHashMap(STRONG, EQUALS, STRONG, IDENTITY, 64);
    final Map<String, MethodExec> opCache = new CustomConcurrentHashMap(STRONG, EQUALS, STRONG, IDENTITY, 64);


    /**
     * TODO maybe use a stack to track invocations inside of evocations inside of invokations etc
     */
    static final ThreadLocal<AtomicBoolean> evoking = ThreadLocal.withInitial(AtomicBoolean::new);

    /**
     * set of operators in probing mode which are kept here for batched execution
     * should be a setAt.   using ConcurrentFastIteratingHashMap instead of the Set because it has newer code updates
     */
    final ConcurrentFastIteratingHashSet<MethodExec> evokeActive =
            new ConcurrentFastIteratingHashSet<>(new MethodExec[0]);



    /**
     * for externally-puppeted method invocation goals
     */


    protected TaskChannel in;
    private final Focus focus;

    private final Memoize<Pair<Pair<Class, Term>, List<Class<?>>>, MethodHandle> methodCache =
            CaffeineMemoize.build(
            //new SoftMemoize<>(
        x -> {

            Class c = x.getOne().getOne();
            Term methodTerm = x.getOne().getTwo();
            List<Class<?>> types = x.getTwo();

            String mName = methodTerm.toString();
            Class<?>[] cc = types.isEmpty() ? ArrayUtil.EMPTY_CLASS_ARRAY : ((Lst<Class<?>>) types).array();
            Method m = findMethod(c, mName, cc);
            if (m == null || !methodEvokable(m))
                return null;

            m.setAccessible(true);
            try {
                MethodHandle mh = MethodHandles.lookup()
                        .unreflect(m)
                        .asFixedArity()
                ;

                return new ConstantCallSite(mh).dynamicInvoker();

                //return mh;

            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
            return null;
        },
    -1 /* soft */, false);
    //, 512, STRONG, SOFT);


    /**
     * whether NAR can envoke the method internally
     */
    public static boolean methodEvokable(Method m) {
        return isPublic(m);
    }


    static boolean isPublic(Method m) {
        return Modifier.isPublic(m.getModifiers());
    }

    public Opjects(Focus w) {
        super($.p(w.id, $.the(Opjects.class)));
        this.focus = w;
    }

    @Override
    protected void starting(NAR nar) {
        in = new TaskChannel(nar.causes.newCause(this));
        next(nar);
        DurLoop on = nar.onDur(Opjects.this::next);
    }

    private final Queue<Class> reflectQueue = new ConcurrentLinkedQueue<>();

    /**
     * called every duration to update all the operators in one batch, so they dont register events individually
     */
    protected void next(NAR nar) {
        //process deferred reflections
        Class c;
        while ((c = reflectQueue.poll())!=null) {
            reflect(c);
        }


        _beliefPri = nar.beliefPriDefault.pri() * beliefPri.floatValue(); //HACK

        double cMin = (float) TruthFunctions.c2e(nar.eviMin());
        double cMax = (float) TruthFunctions.c2e(nar.confDefault(BELIEF));
        beliefEvi = Util.lerpSafe(beliefEviFactor, cMin, cMax);
        //double doubtEvi = Util.lerp(doubtEviFactor, cMin, cMax);
        invokeEvi = Util.lerp(invokeEviFactor, cMin, cMax);
//        float uninvokeEvi = Util.lerp(uninvokeEviFactor, cMin, cMax);
//        float invokePri = beliefPri = pri.floatValue() * nar.priDefault(BELIEF);

        evokeActive.forEachWith(OpExec::update, nar);
    }

    /**
     * registers an alias/binding shortcut target rewrite macro
     */
    public void alias(String op, Term instance, String method) {
        nar.add(Functor.f(op, s ->
            $.func(method, instance, s.subs() == 1 ? s.sub(0) : PROD.the(s))
        ));
    }


    @FunctionalInterface
    interface InstanceMethodValueModel {

        void update(Term instance, Object obj, Method method, Object[] args, Object nextValue, NAR nar);
    }


    final InstanceMethodValueModel pointTasks = new PointMethodValueModel();
    final Function<Term, InstanceMethodValueModel> valueModel = (x) -> pointTasks /* memoryless */;

    public class PointMethodValueModel implements InstanceMethodValueModel {


        @Override
        public void update(Term instance, Object obj, Method method, Object[] args, Object nextValue, NAR nar) {



            long now = nar.time();
            float dur = nar.dur();
            long start = round(now - dur / 2);
            long end = round(now + dur / 2);


            boolean isVoid = method.getReturnType() == void.class;

            Task value = !isVoid ? value(
                    opTerm(instance, method, args, nextValue),
                    beliefFreq, start, end, nar) : null;

            boolean evokedOrInvoked = evoking.get().getOpaque();

            Task feedback = isVoid || evokedOrInvoked ?
                    feedback(
                        opTerm(instance, method, args,
                            isVoid ? null : $.varDep(1)), start, end, nar) : null;

            if (feedback == null && value!=null)
                in.accept(value, focus);
            else if (value==null && feedback != null)
                in.accept(feedback, focus);
            else if (value!=null && feedback!=null)
                in.acceptAll(new Task[]{feedback, value}, focus);

        }

        public Task feedback(Term nt, long start, long end, NAR nar) {

            Task feedback =
//                new TruthletTask(nt, BELIEF,
//                    Truthlet.step(
//                            uninvokeFreq, start,
//                            invokeFreq, invokeEvi,
//                            end, uninvokeFreq,
//                            uninvokeEvi
//                    ), nar);
                    NALTask.taskUnsafe(nt, BELIEF, PreciseTruth.byEvi(invokeFreq, invokeEvi), start, end, nar.evidence());
            //if (NAL.DEBUG) {
            //}
            //feedback.setCyclic(true); //prevent immediate structural transforms
            feedback.priMax(_beliefPri);
            return feedback;
        }


        public Task value(Term nextTerm, float freq, long start, long end, NAR nar) {
            Term nt = nextTerm;
            if (nt instanceof Neg) {
                nt = nt.unneg();
                freq = 1 - freq;
            }

            Task value =
//                new TruthletTask(nt, BELIEF,
//                    Truthlet.step(
//                            doubtFreq, start,
//                            f, beliefEvi,
//                            end, doubtFreq,
//                            doubtEvi
//                    ),
//                    nar);
                    NALTask.taskUnsafe(nt, BELIEF, PreciseTruth.byEvi(freq, beliefEvi), start, end, nar.evidence());


            value.priMax(_beliefPri);
            return value;
        }
    }


    /**
     * this target should not be used in constructing terms that will leave this class.
     * this is so it wont pollute the NAR's index and possibly interfere with other
     * identifiers that it may be equal to (ex: NAR.self())
     */
    private class Instance extends ProxyCompound {

        /**
         * reference to the actual object
         */
        public final Object object;

        final InstanceMethodValueModel belief;

        /**
         * for VM-caused invocations: if true, inputs a goal task since none was involved. assists learning the interface
         */

        Instance(Term id, Object object) {
            /* HACK */
            super((Compound) (id instanceof Atomic ? $.p(id) : id));
            this.object = object;
            this.belief = valueModel.apply(id);
        }

        public Object update(Object obj, Method method, Object[] args, Object nextValue) {


            belief.update(ref, obj, method, args, nextValue, nar);

            return nextValue;
        }


    }

    Term opTerm(Term instance, Method method, Object[] args, Object result) {


        Class<?> returnType = method.getReturnType();
        boolean isVoid = result == null && returnType == void.class;
        boolean isBoolean = returnType == boolean.class || (returnType == Boolean.class && result!=null);

        int xn = 3;
        if (args.length == 0) xn--;
        if (isVoid || isBoolean) xn--;

        Term[] x = new Term[xn];
        int resultTerm = xn - 1;

        x[0] = instance;

        if (method.isVarArgs() && args.length == 1) args = (Object[]) args[0];
        if (args.length > 0) {
            switch (args.length) {
                case 0:
                    break;
                case 1:
                    x[1] = term.term(args[0]);
                    break; /* unwrapped singleton */
                default:
                    x[1] = PROD.the(term.terms(args));
                    break;
            }
            assert (x[1] != null) : "could not termize: " + Arrays.toString(args);
        }

        boolean negate = false;

        if (result instanceof Term tr) {
            if (tr instanceof Neg) {
                tr = tr.unneg();
                negate = true;
            }
            x[resultTerm] = tr;
        } else {
            if (isBoolean) {

                boolean b = (Boolean) result;
                if (!b) negate = true;
                result = null;
            }

            if (!isVoid && !isBoolean) {
                x[resultTerm] = term.term(result);
                assert (x[resultTerm] != null) : "could not termize: " + result;
            }
        }

        return $.func(methodName(method), x).negIf(negate).normalize();

    }

    static String methodName(Method method) {
        String n = method.getName();
        int i = n.indexOf("$accessor$");
        if (i != -1) return n.substring(0, i);
        else return n;
    }

    private class MethodExec extends OpExec implements BiConsumer<Term, Timed> {
        private final Term methodName;
        public Functor functor;

        /**
         * TODO invalidate any entries from here if an instance is registered after it has
         * been running which may cause it to be ignored once it becomes available.
         * maybe use a central runCache to make this easier
         */
        final Memoize<Term, Runnable> runCache;


        MethodExec(String _methodName) {
            super(null, Opjects.this.exeThresh);

            this.methodName = $.atomic(_methodName);

            runCache = CaffeineMemoize.build(term -> {

                Subterms args = validArgs(Functor.args(term));
                if (args == null)
                    return null;

                Term instanceTerm = args.sub(0);
                Object instance = Opjects.this.term.termToObj.get(instanceTerm);
                if (instance == null)
                    return null;

                int as = args.subs();
                Term methodArgs = as > 1 && (as > 2 || !(args.sub(as - 1) instanceof Variable)) ? args.sub(1) : Op.EmptyProduct;

                boolean maWrapped = methodArgs.PROD();

                int aa = maWrapped ? methodArgs.subs() : 1;

                Object[] instanceAndArgs;
                List<Class<?>> types;
                if (aa == 0) {
                    instanceAndArgs = new Object[]{instance};
                    types = Collections.EMPTY_LIST;
                } else {
                    instanceAndArgs = Opjects.this.term.object(instance, maWrapped ? methodArgs.subterms().arrayShared() : new Term[]{methodArgs});
                    types = Util.typesOf(instanceAndArgs, 1 /* skip leading instance value */, instanceAndArgs.length);
                }


                Pair<Pair<Class, Term>, List<Class<?>>> key = pair(pair(instance.getClass(), methodName), types);
                MethodHandle mh = methodCache.apply(key);
                if (mh == null) return null;

                return () -> {

                    AtomicBoolean flag = evoking.get();
                    flag.set(true);

                    try {
                        mh.invokeWithArguments(instanceAndArgs);
                        evoked(methodName, instance, instanceAndArgs);
                    } catch (Throwable throwable) {
                        logger.error("{} execution {}", term, throwable);
                    } finally {
                        flag.set(false);
                    }


                };

            }, -1, false);
            //, 512, STRONG, SOFT);
        }

        @Override
        public int hashCode() {
            return methodName.hashCode();
        }

        @Override
        protected void enable(NAR n) {
            evokeActive.add(this);
        }

        @Override
        protected void disable(NAR n) {
            evokeActive.remove(this);
        }

        @Override
        protected Task exePrefilter(Task x) {

            if (!(x instanceof NALTask) || (((NALTask) x).freq() > 0.5f + FREQ_EPSILON)) {
                Runnable r = runCache.apply(x.term());
                if (r != null) {
                    r.run();
                    return x;
                }
            }
            return null;
        }


        @Override
        public void accept(Term term, Timed timed) {
            runCache.apply(term).run();
        }
    }


    public final <T> T the(String id, T instance, Object... args) {
        return the($.$$(id), instance, args);
    }

    /**
     * wraps a provided instance in an intercepting proxy class
     * not as efficient as the Opject.a(...) method since a custom proxy class will
     * be created, and method invocation is slower, needing to use java reflection.
     */
    public <T> T the(Term id, T instance, Object... args) {

        reflect(instance.getClass());

        try {
            Class cl = bb
                    .with(TypeValidation.DISABLED)
                    .subclass(instance.getClass())
                    .method(ElementMatchers.isPublic().and(ElementMatchers.not(ElementMatchers.isDeclaredBy(Object.class))))

                    .intercept(InvocationHandlerAdapter.of((objWrapper, method, margs) ->
                            invoke(objWrapper, instance, method, margs)))
                    .make()
                    .load(
                            Thread.currentThread().getContextClassLoader(),

                            classLoadingStrategy
                    )
                    .getLoaded();
            T instWrapped = (T) cl.getConstructor(Util.typesOfArray(args)).newInstance(args);

            register(id, instWrapped);

            return instWrapped;


        } catch (NoSuchMethodException | SecurityException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }


    }

    private void reflect(Class<?> cl) {
        clCache.computeIfAbsent(cl, (clazz) -> {
            for (Method m: clazz.getMethods())
                reflect(m);
            return true;
        });
    }

    private MethodExec reflect(Method m) {
        if (!validMethod(m))
            return null;

        m.setAccessible(true);

        String n = m.getName();
        return opCache.computeIfAbsent(n, N -> {
            Atom a = Atomic.atom(N);
            if (nar.concept(a)!=null) {
                /*
                    HACK TODO
                    make 'MethodExec' static class or something that doesnt override behavior among multiple instances from the same functor.. or change argument convention
                */
                throw new UnsupportedOperationException("Functor already registered: " + a);
            }

            MethodExec methodExec = new MethodExec(N);
            methodExec.functor = nar.add(a, methodExec);
            return methodExec;
        });
    }

    public final <T> T a(String id, Class<? extends T> cl, Object... args) {
        return a($.$$(id), cl, args);
    }

    /**
     * instantiates a new instance controlled by this
     */
    public <T> T a(Term id, Class<? extends T> cl, Object... args) {

        Class ccc = proxyCache.computeIfAbsent(cl, (baseClass) -> {

            Class cc = bb
                    .with(TypeValidation.DISABLED)
                    .subclass(baseClass)


                    .method(ElementMatchers.isPublic().and(ElementMatchers.not(ElementMatchers.isDeclaredBy(Object.class))))
                    .intercept(MethodDelegation.to(this))
                    .make()
                    .load(
                            Thread.currentThread().getContextClassLoader(),

                            classLoadingStrategy
                    )
                    .getLoaded();

            reflectQueue.add(cc);

            return cc;
        });


        try {
            T inst = (T) ccc.getConstructor(Util.typesOfArray(args)).newInstance(args);
            return register(id, inst);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }


    }


    @RuntimeType
    public final Object intercept(@AllArguments Object[] args, @SuperMethod Method method, @SuperCall Callable supercall, @This Object obj) {
//        try {
            try {
                Object returned = supercall.call();
                return tryInvoked(obj, method, args, returned);
            } catch (Exception e) {
                //e.printStackTrace();
                throw e instanceof RuntimeException ? ((RuntimeException)e) : new RuntimeException(e);
            }
//
//        } catch (InvocationTargetException | IllegalAccessException e) {
//            logger.error("{} args={}", obj, args);
//            return null;
//        }
    }


    private <T> T register(Term id, T wrappedInstance) {

        term.put(new Instance(id, wrappedInstance), wrappedInstance);

        return wrappedInstance;
    }


    protected boolean evoked(Term method, Object instance, Object[] params) {
        return true;
    }

    protected static Subterms validArgs(Subterms args) {
        int a = args.subs();
        switch (a) {
            case 1:
                return args;
            case 2:
                if (validParamOp(args.sub(1).structOp())) return args;
                break;
            case 3:
                if (validParamOp(args.sub(1).structOp()) && args.sub(2).VAR_DEP()) return args;
                break;
        }
        return null;
    }

    static final int VALID_PARAM_TERM = Op.or(ATOM, INT, VAR_DEP, PROD, BOOL);
    private static boolean validParamOp(int o) {
        return Op.hasAny(VALID_PARAM_TERM, o);
    }


    protected boolean validMethod(Method m) {

        String n = m.getName();
        if (methodExclusions.contains(n) || n.contains("$accessor"))
            return false;
        else {
            int mm = m.getModifiers();

            return Modifier.isPublic(mm) && !Modifier.isStatic(mm);
        }
    }


    private static Method findMethod(Class<?> clazz, Predicate<Method> predicate) {


        for (Class<?> current = clazz; current != null; current = current.getSuperclass()) {



            for (Method method: current.isInterface() ? current.getMethods() : current.getDeclaredMethods())
                if (predicate.test(method)) return method;


            for (Class<?> ifc: current.getInterfaces()) {
                Method m = findMethod(ifc, predicate);
                if (m != null)
                    return m;
            }
        }

        return null;
    }

    /**
     * Determine if the supplied candidate method (typically a method higher in
     * the type hierarchy) has a signature that is compatible with a method that
     * has the supplied name and parameter types, taking method sub-signatures
     * and generics into account.
     */
    private static boolean signaturesCompatible(Method candidate, String method, Class<?>[] parameterTypes) {

        if (!method.equals(candidate.getName())) return false;


        if (parameterTypes.length > 0 && candidate.isVarArgs()) return true;
        if (parameterTypes.length != candidate.getParameterCount()) return false;


        Class<?>[] ctp = candidate.getParameterTypes();
        if (Arrays.equals(parameterTypes, ctp)) return true;


        for (int i = 0; i < parameterTypes.length; i++) {
            Class<?> lowerType = parameterTypes[i];
            Class<?> upperType = ctp[i];
            if (!upperType.isAssignableFrom(lowerType)) return false;
        }


        return true;


    }


    public final Object invoke(Object wrapper, Object obj, Method method, Object[] args) {

        Object result;
        try {
            result = method.invoke(obj, args);
        } catch (Throwable t) {
            logger.error("{} args={}: {}", obj, args, t);
            result = t;
        }

        return tryInvoked(wrapper, method, args, result);
    }


    protected final @Nullable Object tryInvoked(Object obj, Method m, Object[] args, Object result) {
        if (methodExclusions.contains(m.getName()))
            return result;


        return invoked(obj, m, args, result);


    }


    protected Object invoked(Object obj, Method m, Object[] args, Object result) {
        Instance in = (Instance) term.objToTerm.get(obj);
        return (in == null) ?
                result :
                in.update(obj, m, args, result);

    }


    /**
     * @see org.junit.platform.commons.support.ReflectionSupport#findMethod(Class, String, Class...)
     */
    static Method findMethod(Class<?> clazz, String methodName, Class<?>... parameterTypes) {
        Preconditions.notNull(clazz, "Class must not be null");
        Preconditions.notNull(parameterTypes, "Parameter types array must not be null");
        Preconditions.containsNoNullElements(parameterTypes, "Individual parameter types must not be null");

        return findMethod(clazz, method -> signaturesCompatible(method, methodName, parameterTypes));
    }

    private static class MyDefaultTermizer extends DefaultTermizer {
        @Override
        protected Term classInPackage(Term classs, Term packagge) {
            return $.inst(classs, packagge);
        }
    }
}