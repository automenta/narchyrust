package nars.table.question;

import nars.TaskTable;

/**
 * task table used for storing Questions and Quests.
 * simpler than Belief/Goal tables
 */
public interface QuestionTable extends TaskTable {

    /**
     * allows question to pass through it to the link activation phase, but
     * otherwise does not store it
     */
    QuestionTable Empty = new EmptyQuestionTable();


}