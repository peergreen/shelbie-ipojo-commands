package com.peergreen.shelbie.ipojo.internal;

import java.util.Date;
import java.util.List;

import org.apache.felix.gogo.commands.Action;
import org.apache.felix.gogo.commands.Command;
import org.apache.felix.gogo.commands.Option;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.HandlerDeclaration;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.extender.queue.JobInfo;
import org.apache.felix.ipojo.extender.queue.QueueService;
import org.apache.felix.service.command.CommandSession;
import org.fusesource.jansi.Ansi;

@Component
@Command(name = "queue-info",
         scope = "ipojo",
         description = "Show information about the iPOJO's processing queue")
@HandlerDeclaration("<sh:command xmlns:sh='org.ow2.shelbie'/>")
public class QueueInfoAction implements Action {

    private final QueueService queueService;

    @Option(name = "-v",
            aliases = "--verbose",
            description = "When activated, display waiter's details.",
            required = false)
    private boolean verbose = false;

    public QueueInfoAction(final @Requires(filter = "(ipojo.queue.mode=async)") QueueService queueService) {
        this.queueService = queueService;
    }


    public Object execute(final CommandSession session) throws Exception {

        Ansi buffer = Ansi.ansi();
        buffer.render("Executing: @|bold %6d|@ jobs%n", queueService.getCurrents());
        buffer.render("Finished : @|bold %6d|@ jobs%n", queueService.getFinished());
        buffer.render("Waiting  : @|bold %6d|@ jobs%n", queueService.getWaiters());

        if (verbose) {
            // Print waiters details
            List<JobInfo> infos = queueService.getWaitersInfo();
            if (!infos.isEmpty()) {
                buffer.render("%d jobs queued%n", infos.size());
                int index = 1;
                for (JobInfo info : infos) {
                    buffer.render("@|bold %3d|@ %s%n", index++, info.getDescription());
                    buffer.render("    Enlisted at %1$tT %1$tL ms%n", new Date(info.getEnlistmentTime()));
                    buffer.render("    Waiting for %d ms%n", info.getWaitDuration());
                }
            }
        }
        System.out.print(buffer.toString());
        return null;
    }

}