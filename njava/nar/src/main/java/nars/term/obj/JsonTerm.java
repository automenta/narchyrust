package nars.term.obj;

import com.fasterxml.jackson.databind.JsonNode;
import jcog.io.Serials;
import nars.$;
import nars.Term;
import nars.term.atom.Atomic;

import java.util.stream.IntStream;

import static nars.Op.SETe;

/**
 * Created by me on 4/2/17.
 */
public enum JsonTerm { ;


    public static Term the(JsonNode j) {

        if (j.isArray()) {
            return $.p(IntStream.range(0, j.size()).mapToObj(i -> the(j.get(i))).toArray(Term[]::new));

        } else if (j.isValueNode()) {
            if (j.isTextual()) {
                return $.quote(j.textValue());
            } else if (j.isNumber()) {
                return $.the(j.numberValue());
            } else {
                throw new UnsupportedOperationException();
            }
        } else if (j.isObject()) {
            Term[] s = new Term[j.size()];
            int[] i = {0};
            j.fields().forEachRemaining(f -> {
                Atomic k = $.quote(f.getKey());
                Term v = the(f.getValue());
                s[i[0]++] = $.inh(v, k);
            });
            return SETe.the(s);
        } else {
            throw new UnsupportedOperationException("TODO");
        }
    }


    public static Term the(String json) {
        return the(Serials.jsonNode(json));
    }
}