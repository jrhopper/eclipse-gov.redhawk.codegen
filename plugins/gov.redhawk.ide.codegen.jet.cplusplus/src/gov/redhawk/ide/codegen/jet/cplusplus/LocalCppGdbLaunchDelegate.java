/**
 * This file is protected by Copyright.
 * Please refer to the COPYRIGHT file distributed with this source distribution.
 *
 * This file is part of REDHAWK IDE.
 *
 * All rights reserved.  This program and the accompanying materials are made available under
 * the terms of the Eclipse Public License v1.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package gov.redhawk.ide.codegen.jet.cplusplus;

import org.eclipse.cdt.debug.core.ICDTLaunchConfigurationConstants;
import org.eclipse.cdt.dsf.gdb.launching.GdbLaunchDelegate;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;

import gov.redhawk.ide.debug.SpdLauncherUtil;
import mil.jpeojtrs.sca.spd.SoftPkg;

/**
 * Used when a C++ component, device, etc. in the <b>workspace</b> is launched in the sandbox in <b>debug</b> mode.
 * @since 10.2
 */
public class LocalCppGdbLaunchDelegate extends GdbLaunchDelegate {

	public LocalCppGdbLaunchDelegate() {
	}

	@Override
	public ILaunch getLaunch(ILaunchConfiguration configuration, String mode) throws CoreException {
		/**
		 * Create a copy of the launch configuration, and use that to create the ILaunch. This is required for
		 * GdbLaunchDelegate because some areas of the code get the configuration from the ILaunch rather than using
		 * the configuration passed as argument #1 to launch(ILaunchConfiguration, String, ILaunch, IProgressMonitor).
		 * We modify the configuration at launch time with dynamic information (such as the naming context).
		 */
		final ILaunchConfigurationWorkingCopy workingCopy = configuration.getWorkingCopy();
		return super.getLaunch(workingCopy, mode);
	}

	@Override
	public void launch(final ILaunchConfiguration configuration, final String mode, final ILaunch launch, final IProgressMonitor monitor) throws CoreException {
		final int WORK_LAUNCH = 10;
		final int WORK_POST_LAUNCH = 100;
		SubMonitor subMonitor = SubMonitor.convert(monitor, WORK_LAUNCH + WORK_POST_LAUNCH);

		// Validate all XML before doing anything else
		final SoftPkg spd = SpdLauncherUtil.getSpd(configuration);
		IStatus status = SpdLauncherUtil.validateAllXML(spd);
		if (!status.isOK()) {
			throw new CoreException(status);
		}

		// We used a working copy when constructin the ILaunch in getLaunch(ILaunchConfiguration, String)
		ILaunchConfigurationWorkingCopy launchConfiguration = (ILaunchConfigurationWorkingCopy) launch.getLaunchConfiguration();
		insertProgramArguments(spd, launch, launchConfiguration);

		try {
			super.launch(configuration, mode, launch, subMonitor.newChild(WORK_LAUNCH));
			SpdLauncherUtil.postLaunch(spd, configuration, mode, launch, subMonitor.newChild(WORK_POST_LAUNCH));
		} finally {
			if (monitor != null) {
				monitor.done();
			}
		}
	}

	/**
	 * @since 10.0
	 */
	protected void insertProgramArguments(final SoftPkg spd, final ILaunch launch, final ILaunchConfigurationWorkingCopy configuration) throws CoreException {
		final String args = configuration.getAttribute(ICDTLaunchConfigurationConstants.ATTR_PROGRAM_ARGUMENTS, "");

		final String newArgs = SpdLauncherUtil.insertProgramArguments(spd, args, launch, configuration);
		configuration.setAttribute(ICDTLaunchConfigurationConstants.ATTR_PROGRAM_ARGUMENTS, newArgs);
	}
}
