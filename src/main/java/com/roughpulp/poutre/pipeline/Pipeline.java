package com.roughpulp.poutre.pipeline;

import com.google.common.collect.Lists;
import com.roughpulp.commons.cursors.Cursor;
import com.roughpulp.commons.cursors.Cursors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;

public class Pipeline {

	private static final Logger LOGGER = LoggerFactory.getLogger(Pipeline.class);

	public static void run(Processor... processors) throws Exception {
		run(new ArrayList<>(Arrays.asList(processors)));
	}

	public static void run(final Iterable<Processor> processors) throws Exception {
		final ArrayList<Processor> lst = Lists.newArrayList(processors);
		run(lst);
	}

	public static void run(final ArrayList<Processor> processors) throws Exception {
		LOGGER.info("run begin ...");
		try {
			for (final Processor processor : processors) {
				processor.open();
			}
			try {
				final Processor processor = processors.get(0);
				final Cursor out = processor.process(null);
				if (out == null) {
					throw new NullPointerException("Processor returned null: " + makeProcessorName(processor, 0));
				}
				try {
					if (processors.size() > 1) {
						run(processors, out, 1);
					} else {
						Cursors.drain(out);
					}
				} finally {
					out.close();
				}
			} catch (EndOfProcessingException eop) {
				LOGGER.info("reached end of processing");
			} finally {
				for (int ii = 0; ii < processors.size(); ++ii) {
					final Processor processor = processors.get(ii);
					try {
						processor.close();
					} catch (final Exception closeEx) {
						LOGGER.error("exception while closing " + makeProcessorName(processor, ii) + ": " + closeEx, closeEx);
					}
				}
			}
		} finally {
			LOGGER.info("run done");
		}
	}

	private static void run(final ArrayList<Processor> processors, final Cursor source, final int level) throws Exception {
		final Processor processor = processors.get(level);
		Object in = source.next();
		while (in != null) {
			final Cursor out = processor.process(in);
			if (out == null) {
				throw new NullPointerException("Processor returned null: " + makeProcessorName(processor, level));
			}
			try {
				if (level + 1 < processors.size()) {
					run(processors, out, level + 1);
				} else {
					Cursors.drain(out);
				}
				in = source.next();
			} finally {
				out.close();
			}
		}
	}

	private static String makeProcessorName (final Processor processor, final int idx) {
		return "Processor #" + idx + ", class=" + processor.getClass().getName() + ", instance='" + processor.toString() + "'";
	}
}
