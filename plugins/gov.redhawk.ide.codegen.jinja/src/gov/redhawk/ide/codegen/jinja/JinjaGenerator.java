package gov.redhawk.ide.codegen.jinja;

import gov.redhawk.ide.codegen.ImplementationSettings;
import gov.redhawk.ide.codegen.Property;
import gov.redhawk.ide.codegen.jinja.utils.InputRedirector;
import gov.redhawk.model.sca.util.ModelUtil;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import mil.jpeojtrs.sca.spd.Implementation;
import mil.jpeojtrs.sca.spd.SoftPkg;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

public class JinjaGenerator {

	protected List<String> settingsToArguments(final ImplementationSettings implSettings, final SoftPkg softpkg) {
		final List<String> arguments = new ArrayList<String>();
		final IResource resource = ModelUtil.getResource(implSettings);
		final IProject project = resource.getProject();
		final IPath workspaceRoot = project.getWorkspace().getRoot().getLocation();
		final String spdFile = workspaceRoot.toOSString() + softpkg.eResource().getURI().toPlatformString(true);

		arguments.add("--impl");
		arguments.add(implSettings.getId());
		arguments.add("--impldir");
		arguments.add(implSettings.getOutputDir());
		arguments.add("--template");
		arguments.add(implSettings.getTemplate());
		for (final Property property : implSettings.getProperties()) {
			arguments.add("-B" + property.getId() + "=" + property.getValue());
		}
		arguments.add(spdFile);

		return arguments;
	}

	public IStatus generate(final ImplementationSettings implSettings, final Implementation impl, final PrintStream out, final PrintStream err,
	        final IProgressMonitor monitor, final String[] generateFiles) {
		final IResource resource = ModelUtil.getResource(implSettings);
		final IProject project = resource.getProject();
		final SoftPkg softpkg = impl.getSoftPkg();

		final ArrayList<String> arguments = new ArrayList<String>();
		arguments.add("redhawk-codegen");
		arguments.add("-C");
		arguments.add(project.getLocation().toOSString());
		arguments.addAll(settingsToArguments(implSettings, softpkg));
		final String[] command = arguments.toArray(new String[arguments.size()]);

		try {
			final java.lang.Process process = java.lang.Runtime.getRuntime().exec(command);
			if (out != null) {
				// Print the command to the console.
				for (final String arg : command) {
					out.print(arg + " ");
				}
				out.println();

				Thread outThread = new Thread(new InputRedirector(process.getInputStream(), out));
				outThread.start();
			}
			if (err != null) {
				Thread errThread = new Thread(new InputRedirector(process.getErrorStream(), err));
				errThread.start();
			}
			process.waitFor();
		} catch (final Exception e) {
			return new Status(IStatus.ERROR, JinjaGeneratorPlugin.PLUGIN_ID, "Generation failed");
		}
		return new Status(IStatus.OK, JinjaGeneratorPlugin.PLUGIN_ID, "Generation complete");
	}

	public HashMap<String, Boolean> getGeneratedFiles(final ImplementationSettings implSettings, final SoftPkg softpkg) throws CoreException {
		final HashMap<String, Boolean> fileList = new HashMap<String, Boolean>();

		final ArrayList<String> arguments = new ArrayList<String>();
		arguments.add("redhawk-codegen");
		arguments.add("-l");
		arguments.addAll(settingsToArguments(implSettings, softpkg));
		final String[] command = arguments.toArray(new String[arguments.size()]);

		try {
			final java.lang.Process process = java.lang.Runtime.getRuntime().exec(command);
			final InputStreamReader instream = new InputStreamReader(process.getInputStream());
			final BufferedReader reader = new BufferedReader(instream);
			String line;
			while ((line = reader.readLine()) != null) {
				fileList.put(line, true);
			}
		} catch (final Exception e) {
			return null;
		}
		return fileList;
	}

	public IStatus validate() {
		return new Status(IStatus.OK, JinjaGeneratorPlugin.PLUGIN_ID, "Validation ok");
	}
}
