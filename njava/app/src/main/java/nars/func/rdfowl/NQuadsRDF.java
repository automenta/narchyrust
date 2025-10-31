package nars.func.rdfowl;

import com.google.common.collect.Streams;
import nars.*;
import nars.task.TaskBuilder;
import nars.term.Compound;
import nars.term.atom.Atom;
import nars.term.atom.Atomic;
import org.jetbrains.annotations.Nullable;
import org.semanticweb.yars.nx.Node;
import org.semanticweb.yars.nx.parser.NxParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.namespace.QName;
import java.io.*;
import java.util.*;
import java.util.stream.Stream;

import static nars.$.*;
import static nars.Op.*;

/**
 * Created by me on 6/4/15.
 */
public enum NQuadsRDF {
	;


	public static final Atomic owlClass = Atomic.atomic("Class");
	public static final Set<Atomic> predicatesIgnored = new HashSet() {{
		add(Atomic.atomic("comment"));
		add(Atomic.atomic("isDefinedBy"));
	}};
	static final Atomic parentOf = Atomic.atomic("parentOf");
	static final Atomic type = Atomic.atomic("type");
	static final Atomic subClassOf = Atomic.atomic("subClassOf");
	static final Atomic isPartOf = Atomic.atomic("isPartOf");
	static final Atomic subPropertyOf = Atomic.atomic("subPropertyOf");
	static final Atomic equivalentClass = Atomic.atomic("equivalentClass");
	static final Atomic equivalentProperty = Atomic.atomic("equivalentProperty");
	static final Atomic inverseOf = Atomic.atomic("inverseOf");
	static final Atomic disjointWith = Atomic.atomic("disjointWith");
	static final Atomic domain = Atomic.atomic("domain");
	static final Atomic range = Atomic.atomic("range");
	static final Atomic sameAs = Atomic.atomic("sameAs");
	static final Atomic differentFrom = Atomic.atomic("differentFrom");
	static final Atomic dataTypeProperty = Atomic.atomic("DatatypeProperty");
	static final Atomic objProperty = Atomic.atomic("ObjectProperty");
	static final Atomic funcProp = Atomic.atomic("FunctionalProperty");
	static final Atomic invFuncProp = Atomic.atomic("InverseFunctionalProperty");
	static final Logger logger = LoggerFactory.getLogger(NQuadsRDF.class);

	public static void input(NAR nar, String input) {
		input(nar, new NxParser(List.of(input)));
	}

	public static void input(NAR nar, InputStream input) {

		input(nar, new NxParser(input));
	}

	@Deprecated
	public static void input(NAR nar, Iterable<Node[]> nxp) {
		input(nar, Streams.stream(nxp));
	}

	public static Stream<Node[]> stream(InputStream input) {
		NxParser p = new NxParser(input);
		try {
			input.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return Streams.stream((Iterable<Node[]>) p);
	}

	@Deprecated
	public static void input(NAR nar, Stream<Node[]> nxp) {
		nar.input(stream(nar, nxp));
	}

	public static Stream<Task> stream(NAR n, File f) throws FileNotFoundException {
		return NQuadsRDF.stream(n, NQuadsRDF.stream(f));
	}

	public static Stream<Task> stream(NAR nar, Stream<Node[]> nxp) {

		return nxp.map(nx -> {
			if (nx.length >= 3) {


				return inputNALlike(
					nar,
					resource(nx[0]),
					resource(nx[1]),
					resource(nx[2])
				);

			}
			return null;
		}).filter(Objects::nonNull);
	}

	public static Atomic resource(Node n) {
		String s = n.getLabel();


		if (s.contains("#")) {
			String[] a = s.split("#");

			s = a[1];
		} else {
			String[] a = s.split("/");
			if (a.length == 0) return null;
			s = a[a.length - 1];
		}

		if (s.isEmpty()) return null;

		try {
			return Atomic.atomic(s);
		} catch (Exception e) {
			return $.quote(s);
		}


	}

	public static Term atom(String uri) {
		int lastSlash = uri.lastIndexOf('/');
		if (lastSlash != -1)
			uri = uri.substring(lastSlash + 1);

		switch (uri) {
			case "owl#Thing" -> uri = "thing";
		}


		return Atomic.atomic(uri);


	}

	static @Nullable Term subjObjInh(Term subject, char subjType, char objType, boolean reverse) {
		String a = reverse ? "subj" : "obj";
		String b = reverse ? "obj" : "subj";
		return inh(
			p(v(subjType, a), v(objType, b)),
			subject);
	}

	public static Task inputRaw(NAR nar,
								@Nullable Atom subject,
								Atom predicate, Term object) {

		if (subject == null)
			return null;

		if (predicatesIgnored.contains(predicate))
			return null;

		try {
			Term term = /*$.inst*/ $.inh($.p(subject, object), predicate);
			if (term == null)
				throw new NullPointerException();
			Task t = new TaskBuilder(term, BELIEF, $.t(1f, nar.confDefault(BELIEF))).apply(nar);
			return t;
		} catch (Exception e) {
			logger.error("rdf({}) to task: {}", new Term[]{subject, object, predicate}, e);
			return null;
		}

	}

	/**
	 * Saves the relation into the database. Both entities must exist if the
	 * relation is to be saved. Takes care of updating relation_types as well.
	 */
	public static Task inputNALlike(NAR nar,
									Atomic subject,
									Atomic predicate, @Nullable Term object) {


		if (predicatesIgnored.contains(predicate))
			return null;

		Term belief = null;
		if (Arrays.asList(type, subClassOf, subPropertyOf).contains(predicate)) {
			if (object.equals(owlClass)) {
				belief = $.inst($.varDep(1), subject);
			} else if (List.of(dataTypeProperty, funcProp, invFuncProp, objProperty).contains(object)
			) {
				return null;
			} else {


				belief = inh(subject, object);
			}

		} else if ((predicate.equals(parentOf))) {


		} else if (predicate.equals(equivalentClass)) {

			belief = equi(
				inst(varIndep("subj"), subject),
				inst(varIndep("pred"), object)
			);
		} else if (predicate.equals(isPartOf)) {
			belief = $.instprop(subject, object);
		} else if (predicate.equals(sameAs)) {
			belief = sim(subject, object);
		} else if (predicate.equals(differentFrom)) {
			belief = sim(subject, object).neg();
		} else if (predicate.equals(domain)) {


			belief = $.impl(
				$.func(subject, $.varIndep(1), $.varDep(2)),
				$.inst($.varIndep(1), object)
			);

		} else if (predicate.equals(range)) {
			belief = $.impl(
				$.func(subject, $.varDep(2), $.varIndep(1)),
				$.inst($.varIndep(1), object)
			);


		} else if (predicate.equals(equivalentProperty)) {

			belief = sim(subject, object);
		} else if (predicate.equals(inverseOf)) {


			belief = equi(
				subjObjInh(subject, '$', '$', false),
				subjObjInh(object, '$', '$', true));

		} else if (predicate.equals(disjointWith)) {


			belief = $.inst($.varDep(1),
				CONJ.the(subject, object)
			).neg();


		} else {
			if (subject != null && object != null && predicate != null) {
				belief =

					inh(
						p(subject, object),
						predicate
					);
			}
		}

		if (belief instanceof Compound) {

			return NALTask.taskUnsafe(belief, BELIEF, $.t(1f, nar.confDefault(BELIEF)), ETERNAL, ETERNAL, nar.evidence())
				.pri(nar)
				;


//                    .eternal().pri(nar).apply(nar);
		}

		return null;
	}

	public static Term equi(Term x, Term y) {


		return CONJ.the(
			IMPL.the(x, y),
			IMPL.the(y, x)
		);
	}

	public static Term disjoint(Term x, Term y) {


		return CONJ.the(
			CONJ.the(x, y).neg(),
			CONJ.the(x.neg(), y.neg()).neg()
		);
	}

	/**
	 * Format the XML tag. Takes as input the QName of the tag, and formats it
	 * to a namespace:tagname format.
	 *
	 * @param qname the QName for the tag.
	 * @return the formatted QName for the tag.
	 */
	private static String formatTag(QName qname) {
		String prefix = qname.getPrefix();
		String suffix = qname.getLocalPart();

		suffix = suffix.replace("http://dbpedia.org/ontology/", "");

		return prefix == null || prefix.isEmpty() ? suffix : prefix + ':' + suffix;
	}

	/**
	 * Split up Uppercase Camelcased names (like Java classnames or C++ variable
	 * names) into English phrases by splitting wherever there is a transition
	 * from lowercase to uppercase.
	 *
	 * @param name the input camel cased name.
	 * @return the "english" name.
	 */
	private static String getEnglishName(String name) {
		StringBuilder englishNameBuilder = new StringBuilder();
		char[] namechars = name.toCharArray();
		for (int i = 0; i < namechars.length; i++) {
			if (i > 0 && Character.isUpperCase(namechars[i])
				&& Character.isLowerCase(namechars[i - 1])) {
				englishNameBuilder.append(' ');
			}
			englishNameBuilder.append(namechars[i]);
		}
		return englishNameBuilder.toString();
	}

	@Deprecated
	public static void input(NAR n, File f) throws FileNotFoundException {
		logger.info("loading {}", f);
		input(n, new BufferedInputStream(new FileInputStream(f)));

	}

	public static Stream<Node[]> stream(File f) throws FileNotFoundException {
		logger.info("loading {}", f);
		return stream(new BufferedInputStream(new FileInputStream(f)));
	}


}