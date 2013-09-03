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

import java.util.Map;
import java.util.TreeMap;

import org.apache.felix.gogo.commands.Command;
import org.apache.felix.gogo.commands.Option;
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
@Command(name = "declarations",
         scope = "ipojo",
         description = "List declarations registered on the system")
@HandlerDeclaration("<sh:command xmlns:sh='org.ow2.shelbie'/>")
public class DeclarationsAction extends AbstractDeclarationAction {

    @Option(name = "-w",
            aliases = "--very-verbose",
            description = "When activated, bound declarations are also displayed",
            required = false)
    private boolean veryVerbose = false;


    private Map<ServiceReference, Declaration> declarations = new TreeMap<ServiceReference, Declaration>();

    public Object execute(final CommandSession session) throws Exception {

        Ansi buffer = Ansi.ansi();

        int bound = 0;
        int unbound = 0;
        for (Map.Entry<ServiceReference, Declaration> entry : declarations.entrySet()) {
            Declaration declaration = entry.getValue();
            if (!declaration.getStatus().isBound()) {
                printDeclarationLine(buffer, entry.getKey(), declaration);
                unbound++;
            } else {
                if (veryVerbose) {
                    printDeclarationLine(buffer, entry.getKey(), declaration);
                }
                bound++;
            }
        }

        Ansi decorate = Ansi.ansi();
        decorate.render("@|green %d|@ Declaration(s) are bound%n", bound);
        decorate.render("@|yellow %d|@ Declaration(s) are unbound%n", unbound);
        if (!buffer.toString().isEmpty()) {
            decorate.render("@|bold Bnd |   ID |                 Type |  Status | Message|@");
            decorate.newline();
            decorate.a(buffer);
        }


        System.out.print(decorate);
        return null;
    }

    private void printDeclarationLine(final Ansi buffer, final ServiceReference reference, final Declaration declaration) {

        long bundleId = reference.getBundle().getBundleId();
        long serviceId = (Long) reference.getProperty(Constants.SERVICE_ID);
        String type = declaration.getClass().getSimpleName();
        buffer.render("%-3d | %-4d | %20s | ", bundleId, serviceId, type.substring(0, 20));
        Status status = declaration.getStatus();
        printColoredStatus(buffer, status);
        buffer.render(" | %s%n", status.getMessage());

        Throwable throwable = status.getThrowable();
        if (throwable != null) {
            printThrowable(buffer, throwable);
        }
        printDetails(buffer, declaration);
    }

    @Bind(aggregate = true, optional = true)
    public void bindTypeDeclaration(TypeDeclaration declaration, ServiceReference reference) {
        declarations.put(reference, declaration);
    }

    @Unbind
    public void unbindTypeDeclaration(ServiceReference reference) {
        declarations.remove(reference);
    }

    @Bind(aggregate = true, optional = true)
    public void bindInstanceDeclaration(InstanceDeclaration declaration, ServiceReference reference) {
        declarations.put(reference, declaration);
    }

    @Unbind
    public void unbindInstanceDeclaration(ServiceReference reference) {
        declarations.remove(reference);
    }

    @Bind(aggregate = true, optional = true)
    public void bindExtensionDeclaration(ExtensionDeclaration declaration, ServiceReference reference) {
        declarations.put(reference, declaration);
    }

    @Unbind
    public void unbindExtensionDeclaration(ServiceReference reference) {
        declarations.remove(reference);
    }

}