package com.peergreen.shelbie.ipojo.internal;

import java.util.Collections;

import org.apache.felix.gogo.commands.Action;
import org.apache.felix.gogo.commands.Option;
import org.apache.felix.ipojo.extender.Declaration;
import org.apache.felix.ipojo.extender.ExtensionDeclaration;
import org.apache.felix.ipojo.extender.InstanceDeclaration;
import org.apache.felix.ipojo.extender.Status;
import org.apache.felix.ipojo.extender.TypeDeclaration;
import org.fusesource.jansi.Ansi;

public abstract class AbstractDeclarationAction implements Action {

    @Option(name = "-v",
            aliases = "--verbose",
            description = "When activated, display additional declaration details",
            required = false)
    protected boolean verbose = false;


    protected void printDetails(final Ansi buffer, final Declaration declaration) {
        if (declaration instanceof InstanceDeclaration) {
            InstanceDeclaration id = (InstanceDeclaration) declaration;
            if (!InstanceDeclaration.UNNAMED_INSTANCE.equals(id.getInstanceName())) {
                buffer.render("  %-15s @|faint %s|@%n", "Name", id.getInstanceName());
            }
            buffer.render("  %-15s @|faint %s|@%n", "Component", id.getComponentName());
            if (id.getComponentVersion() != null) {
                buffer.render("  %-15s @|faint %s|@%n", "Version", id.getComponentVersion());
            }

            if (verbose) {
                if (!id.getConfiguration().isEmpty()) {
                    buffer.render("  Configuration properties");
                    buffer.newline();
                    for (String key : Collections.list(id.getConfiguration().keys())) {
                        buffer.render("  * %-15s @|faint %s|@%n", key, id.getConfiguration().get(key));
                    }

                }
            }
        } else if (declaration instanceof TypeDeclaration) {
            TypeDeclaration td = (TypeDeclaration) declaration;
            buffer.render("  %-15s @|faint %s|@%n", "Name", td.getComponentName());
            if (verbose) {
                buffer.render("  %-15s @|faint %s|@%n", "Public", td.isPublic());
                if (td.getComponentVersion() != null) {
                    buffer.render("  %-15s @|faint %s|@%n", "Version", td.getComponentVersion());
                }
                buffer.render("  %-15s @|faint %s|@%n", "Requires", td.getExtension());
            }
        } else if (declaration instanceof ExtensionDeclaration) {
            ExtensionDeclaration ed = (ExtensionDeclaration) declaration;
            buffer.render("  %-15s @|faint %s|@%n", "Name", ed.getExtensionName());
        }
    }

    protected void printThrowable(final Ansi buffer, final Throwable throwable) {
        buffer.render("  @|red,bold %s|@: %s%n", throwable.getClass().getName(), throwable.getMessage());
        if (verbose) {
            for (StackTraceElement element : throwable.getStackTrace()) {
                buffer.render("    %s%n", element);
            }
            if (throwable.getCause() != null) {
                buffer.render("@|bold Caused by |@");
                printThrowable(buffer, throwable.getCause());
            }
        }
    }

    protected void printColoredStatus(final Ansi buffer, final Status status) {
        if (status.isBound()) {
            buffer.render("@|green BOUND|@");
        } else {
            buffer.render("@|red UNBOUND|@");
        }
    }
}