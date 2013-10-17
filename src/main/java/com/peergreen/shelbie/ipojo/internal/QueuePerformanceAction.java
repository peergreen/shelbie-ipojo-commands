/**
 * Copyright 2013 Peergreen S.A.S. All rights reserved.
 * Proprietary and confidential.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.peergreen.shelbie.ipojo.internal;

import static java.lang.String.format;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.felix.gogo.commands.Action;
import org.apache.felix.gogo.commands.Command;
import org.apache.felix.gogo.commands.Option;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.HandlerDeclaration;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.extender.queue.JobInfo;
import org.apache.felix.ipojo.extender.queue.QueueListener;
import org.apache.felix.ipojo.extender.queue.debug.QueueEventProxy;
import org.apache.felix.service.command.CommandSession;
import org.fusesource.jansi.Ansi;

@Component
@Command(name = "queue-performance",
         scope = "ipojo",
         description = "Show information about the iPOJO's processing queue")
@HandlerDeclaration("<sh:command xmlns:sh='org.ow2.shelbie'/>")
public class QueuePerformanceAction implements Action {

    @Option(name = "-w",
            aliases = "--worst",
            description = "Number of worst jobs to display (5 by default)",
            required = false)
    private int worsts = 5;

    private final QueueEventProxy m_eventProxy;

    public QueuePerformanceAction(final @Requires QueueEventProxy m_eventProxy) {
        this.m_eventProxy = m_eventProxy;
    }

    public Object execute(final CommandSession session) throws Exception {

        AccumulatedDurationListener accumulated = new AccumulatedDurationListener();
        PerJobTypeAccumulator partitioned = new PerJobTypeAccumulator();
        WorstJobsFinder worstJobsFinder = new WorstJobsFinder(worsts);
        m_eventProxy.addQueueListener(accumulated);
        m_eventProxy.addQueueListener(partitioned);
        m_eventProxy.addQueueListener(worstJobsFinder);

        Ansi buffer = Ansi.ansi();

        printBanner(buffer, "Summary (globally accumulated times)");

        // Prints totals
        buffer.render("Executed @|bold %6d|@ jobs%n", accumulated.numberOfJobs);
        buffer.render("Total execution: @|bold %6d|@ ms (avg:%4d, med:%4d)%n",
                      accumulated.total,
                      (accumulated.total / accumulated.numberOfJobs),
                      median(accumulated.totalValues));
        buffer.render("Total waiting  : @|bold %6d|@ ms (avg:%4d, med:%4d)%n",
                      accumulated.waiting,
                      (accumulated.waiting / accumulated.numberOfJobs),
                      median(accumulated.waitingValues));

        buffer.newline();

        printBanner(buffer, "Per job-type partitions");

        // Prints per-jobtype
        for (PerJobInfos partition : partitioned.partitions.values()) {
            buffer.render("@|bold %s|@ / %d jobs%n", partition.jobType, partition.numberOfJobs);
            buffer.render("  Total execution: @|bold %6d|@ ms (min:%4d, max:%4d, avg:%4d, med:%4d)%n",
                          partition.execution,
                          partition.minExecution,
                          partition.maxExecution,
                          (partition.execution / partition.numberOfJobs),
                          median(partition.totalValues));
            buffer.render("  Total waiting  : @|bold %6d|@ ms (min:%4d, max:%4d, avg:%4d, med:%4d)%n",
                          partition.waiting,
                          partition.minWaiting,
                          partition.maxWaiting,
                          (partition.waiting / partition.numberOfJobs),
                          median(partition.waitingValues));
        }

        buffer.newline();

        printBanner(buffer, format("%d worst jobs (most time consumers)", worstJobsFinder.size));

        Collections.sort(worstJobsFinder.worsts, Collections.reverseOrder(new WorstJobInfoComparator()));
        int index = 0;
        for (JobInfo info : worstJobsFinder.worsts) {
            buffer.render("%3d [@|bold %s|@] %s%n", index++, info.getJobType(), info.getDescription());
            buffer.render("    Executed in @|bold,red %d|@ ms%n", info.getExecutionDuration());
            buffer.render("    Enlisted at %1$tT %1$tL ms%n", new Date(info.getEnlistmentTime()));
            buffer.render("    Waited  for %d ms%n", info.getWaitDuration());
        }

        System.out.print(buffer.toString());

        m_eventProxy.removeQueueListener(accumulated);
        m_eventProxy.removeQueueListener(partitioned);
        m_eventProxy.removeQueueListener(worstJobsFinder);

        return null;
    }

    private long median(final List<Long> values) {
        if (values.isEmpty()) {
            return 0;
        }
        return values.get((values.size() + 1) / 2);
    }

    private void printBanner(final Ansi buffer, final String title) {
        buffer.render("@|bold ------------------------------------------------------|@");
        buffer.newline();
        buffer.render("@|bold  > %s|@%n", title);
        buffer.render("@|bold ------------------------------------------------------|@");
        buffer.newline();
    }


    private static class EmptyQueueListener implements QueueListener {

        @Override
        public void enlisted(final JobInfo info) {

        }

        @Override
        public void started(final JobInfo info) {

        }

        @Override
        public void executed(final JobInfo info, final Object o) {
            ended(info);
        }

        @Override
        public void failed(final JobInfo info, final Throwable throwable) {
            ended(info);
        }

        protected void ended(final JobInfo info) {

        }
    }

    private static class WorstJobInfoComparator implements Comparator<JobInfo> {
        @Override
        public int compare(final JobInfo first, final JobInfo second) {
            if (first.getExecutionDuration() > second.getExecutionDuration()) {
                return 1;
            }
            if (first.getExecutionDuration() < second.getExecutionDuration()) {
                return -1;
            }

            // do not return 0, otherwise one of the 2 elements is crushed (removed from the list)
            return (int) (first.getWaitDuration() - second.getWaitDuration());
        }
    }

    private static class AccumulatedDurationListener extends EmptyQueueListener {

        private long total = 0;
        private long waiting = 0;
        private int numberOfJobs = 0;
        private List<Long> totalValues = new ArrayList<>();
        private List<Long> waitingValues = new ArrayList<>();

        @Override
        public void started(final JobInfo info) {
            long waitDuration = info.getWaitDuration();
            waiting += waitDuration;
            waitingValues.add(waitDuration);
            numberOfJobs++;
        }

        @Override
        protected void ended(final JobInfo info) {
            long executionDuration = info.getExecutionDuration();
            total += executionDuration;
            totalValues.add(executionDuration);
        }

    }

    private static class PerJobTypeAccumulator extends EmptyQueueListener {
        private Map<String, PerJobInfos> partitions = new HashMap<>();

        @Override
        public void started(final JobInfo info) {
            PerJobInfos infos = getPerJobInfos(info);
            long waitDuration = info.getWaitDuration();
            infos.waiting += waitDuration;
            infos.waitingValues.add(waitDuration);
            infos.numberOfJobs++;

            if (infos.maxWaiting < waitDuration) {
                infos.maxWaiting = waitDuration;
            }

            if (infos.minWaiting > waitDuration) {
                infos.minWaiting = waitDuration;
            }
        }

        private PerJobInfos getPerJobInfos(final JobInfo info) {
            if (!partitions.containsKey(info.getJobType())) {
                partitions.put(info.getJobType(), new PerJobInfos(info.getJobType()));
            }
            return partitions.get(info.getJobType());
        }

        @Override
        protected void ended(final JobInfo info) {
            PerJobInfos infos = getPerJobInfos(info);
            long executionDuration = info.getExecutionDuration();
            infos.totalValues.add(executionDuration);
            infos.execution += executionDuration;

            if (infos.maxExecution < executionDuration) {
                infos.maxExecution = executionDuration;
            }

            if (infos.minExecution > executionDuration) {
                infos.minExecution = executionDuration;
            }

        }

    }

    private static class PerJobInfos {
        private final String jobType;
        long execution = 0;
        long minExecution = Integer.MAX_VALUE;
        long maxExecution = Integer.MIN_VALUE;
        long waiting = 0;
        long minWaiting = Integer.MAX_VALUE;
        long maxWaiting = Integer.MIN_VALUE;
        int numberOfJobs = 0;
        List<Long> totalValues = new ArrayList<>();
        List<Long> waitingValues = new ArrayList<>();

        public PerJobInfos(final String jobType) {
            this.jobType = jobType;
        }
    }

    private static class WorstJobsFinder extends EmptyQueueListener {
        private final int size;

        private List<JobInfo> worsts = new ArrayList<>();

        public WorstJobsFinder(final int size) {
            this.size = size;
        }

        @Override
        protected void ended(final JobInfo info) {
            if (worsts.size() < size) {
                worsts.add(info);
            } else {
                JobInfo minimal = findMinimalJob();
                long minimum = (minimal == null) ? Long.MIN_VALUE : minimal.getExecutionDuration();
                if (info.getExecutionDuration() > minimum) {
                    add(info);
                }
            }
        }

        private void add(final JobInfo info) {
            // Remove worst element
            worsts.remove(findMinimalJob());
            worsts.add(info);
        }

        private JobInfo findMinimalJob() {
            JobInfo minimal = null;
            long minimum = Long.MAX_VALUE;
            for (JobInfo info : worsts) {
                if (info.getExecutionDuration() < minimum) {
                    minimum = info.getExecutionDuration();
                    minimal = info;
                }
            }
            return minimal;
        }
    }
}