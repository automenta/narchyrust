package nars.nal.nal3;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class NAL3DecomposeQuestionTest extends NAL3Test {

    @ParameterizedTest
    @ValueSource(strings = {"|", "&"})
    void questionDecomposition0(String o) {
        test
                .volMax(7)
                .question("((swan" + o + "swimmer) --> bird)")
                .mustQuestion(cycles, "(swimmer --> bird)")
                .mustQuestion(cycles, "(swan --> bird)")
        ;
    }

    @Test
    void decomposeQuestion() {
        test
                .volMax(6)
                .question("(x-->(a & b))")
                .mustQuestion(cycles, "(x-->a)")
                .mustQuestion(cycles, "(x-->b)");
    }

    @Test
    void decomposeQuestion_B_pred() {
        test
                .volMax(5)
                .believe("(x-->(a&b)).")
                .question("(x-->a)")
                //.mustQuestion(cycles, "(x-->(a&b))");
                .mustQuestion(cycles, "(x-->b)");
    }

    @Test
    void decomposeQuestion_B_subj() {
        test
                .volMax(5)
                .believe("((a&b)-->x).")
                .question("(a-->x)")
                //.mustQuestion(cycles, "((a&b)-->x)");
                .mustQuestion(cycles, "(b-->x)");
    }

    @Test void inhQuestionDecompose1() {
       test
           .volMax(6)
           .input("((a&b)-->x)?")
           .mustQuestion(cycles, "(a-->x)");
    }
    @Test void simQuestionDecompose1() {
        test
                .volMax(6)
                .input("((a&b)<->x)?")
                .mustQuestion(cycles, "(a<->x)");
    }
    @Test void inhQuestDecompose1() {
        test
                .volMax(6)
                .input("((a&b)-->x)@")
                .mustQuest(cycles, "(a-->x)");
    }
}