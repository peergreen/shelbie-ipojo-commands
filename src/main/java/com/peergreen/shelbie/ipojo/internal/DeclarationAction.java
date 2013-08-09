package com.peergreen.shelbie.ipojo.internal;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.apache.felix.ipojo.annotations.Bind;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.HandlerDeclaration;
import org.apache.felix.ipojo.annotations.Unbind;
import org.apache.felix.ipojo.extender.Declaration;
import org.apache.felix.ipojo.extender.ExtensionDeclaration;
import org.apache.felix.ipojo.extender.InstanceDeclaration;
import org.apache.felix.ipojo.extender.Status;
import org.apache.felix.ipojo.extender.TypeDeclaration;
import org.apache.felix.service.command.CommandSession;
import org.fusesource.jansi.Ansi;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;

@Component
@Command(name = "declaration",
         scope = "ipojo",
         description = "Display iPOJO's declaration details")
@HandlerDeclaration("<sh:command xmlns:sh='org.ow2.shelbie'/>")
public class DeclarationAction extends AbstractDeclarationAction {

    private Map<Long, Declaration> declarations = new TreeMap<Long, Declaration>();

    @Argument(name = "service-ids",
              description = "Service ID of the declarations to display",
              multiValued = true,
              required = true)
    private List<Long> serviceIds;

    public Object execute(final CommandSession session) throws Exception {

        verbose = true;
        Ansi buffer = Ansi.ansi();

        for (Long serviceId : serviceIds) {
            Declaration declaration = declarations.get(serviceId);
            if (declaration == null) {
                buffer.render("@|bold,red Service ID %d do not store a Declaration|@%n", serviceId);
            } else {

                // Header
                // -----------------
                //buffer.render("Declaration %d (from bundle %d '%s') is ", serviceId);
                buffer.render("Declaration %d is ", serviceId);
                Status status = declaration.getStatus();
                printColoredStatus(buffer, status);
                buffer.newline();

                // Elements
                // -----------------
                buffer.render("  %-15s %s%n", "Implementation", declaration.getClass().getName());
                buffer.render("  %-15s %s%n", "Message", status.getMessage());
                buffer.render("  %-15s ", "Status");
                printColoredStatus(buffer, status);
                buffer.newline();
                printDetails(buffer, declaration);
                if (status.getThrowable() != null) {
                    printThrowable(buffer, status.getThrowable());
                }
            }
        }

        System.out.print(buffer);
        return null;
    }

    @Bind(aggregate = true, optional = true)
    public void bindTypeDeclaration(TypeDeclaration declaration, ServiceReference reference) {
        long serviceId = (Long) reference.getProperty(Constants.SERVICE_ID);
        declarations.put(serviceId, declaration);
    }

    @Unbind
    public void unbindTypeDeclaration(ServiceReference reference) {
        long serviceId = (Long) reference.getProperty(Constants.SERVICE_ID);
        declarations.remove(serviceId);
    }

    @Bind(aggregate = true, optional = true)
    public void bindInstanceDeclaration(InstanceDeclaration declaration, ServiceReference reference) {
        long serviceId = (Long) reference.getProperty(Constants.SERVICE_ID);
        declarations.put(serviceId, declaration);
    }

    @Unbind
    public void unbindInstanceDeclaration(ServiceReference reference) {
        long serviceId = (Long) reference.getProperty(Constants.SERVICE_ID);
        declarations.remove(serviceId);
    }

    @Bind(aggregate = true, optional = true)
    public void bindExtensionDeclaration(ExtensionDeclaration declaration, ServiceReference reference) {
        long serviceId = (Long) reference.getProperty(Constants.SERVICE_ID);
        declarations.put(serviceId, declaration);
    }

    @Unbind
    public void unbindExtensionDeclaration(ServiceReference reference) {
        long serviceId = (Long) reference.getProperty(Constants.SERVICE_ID);
        declarations.remove(serviceId);
    }

}