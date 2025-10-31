package nars.util;

import jcog.signal.meter.Metered;
import jcog.table.ARFF;
import jcog.table.DataTable;
import nars.Concept;
import nars.Task;
import nars.Term;
import tech.tablesaw.api.Table;
import tech.tablesaw.columns.Column;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static jcog.Util.maybeEqual;

/** experience snapshot */
public class TaskSummary implements Metered {

	final Map metadata = new TreeMap();

	private final Set<Task> _tasks = new HashSet();
	private final DataTable table = new ARFF();

	public TaskSummary() {
		metadata.put("creation", new Date());
	}

	public TaskSummary(Object name) {
		this();
		metadata.put("name", name);
	}

	public TaskSummary put(Object key, Object value) {
		metadata.put(key, value);
		return this;
	}

	public TaskSummary addConceptTasks(Stream<Concept> s) {
		return add(s.flatMap(Concept::tasks));
	}

	public TaskSummary add(Stream<Task> s) {
		s.forEach(_tasks::add);
		return this;
	}

	public synchronized TaskSummary reindex() {
		if (table.rowCount() == _tasks.size())
			return this; //HACK

		table.clear();

		if (table.columnCount()==0)
			initIndex();

		for (Task x : _tasks) {
			Term X = x.term();
			String termStr = X.toString();
			table.add(termStr, maybeEqual(X.root().toString(), termStr),
					x.priElseZero(), String.valueOf((char)x.punc()), X.complexity());
		}
		//System.out.println(table.summary());
		return this;
	}

	private void initIndex() {
		table.defineText("term");
		table.defineText("termRoot");
		table.defineNumeric("pri");
		table.defineText("punc");
		table.defineNumeric("volume");
		//table.defineNumeric("conf");
	}

	@Override
	public Map<String, Consumer<MeterReader>> metrics() {
		return tableAggregates(table);
	}

	public static Map<String, Consumer<MeterReader>> tableAggregates(Table table) {
		Map<String, Consumer<MeterReader>> m = new TreeMap();
		for (Column c : table.columns()) {
			c.summary().forEach(r -> m.put(c.name() + "...", (x)-> { } /* TODO */));
		}
		return m;
	}
}