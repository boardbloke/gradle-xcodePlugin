/*
 * Copyright 2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openbakery

import org.gradle.api.Action
import org.gradle.api.Task
import org.gradle.api.tasks.TaskAction

/**
 *
 * @author René Pirringer
 *
 */
class CodesignTask extends AbstractXcodeTask {

	CodesignTask() {
		super()
		dependsOn("xcodebuild")
		this.description = "Signs the app bundle that was created by xcodebuild"
	}



	@TaskAction
	def codesign() {
		if (!project.xcodebuild.sdk.startsWith("iphoneos")) {
			println("not a device build, so no codesign needed")
			return
		}
		if (project.xcodebuild.signing == null) {
			throw new IllegalArgumentException("cannot signed with unknown signing configuration")
		}

		println project.xcodebuild.symRoot
		def buildOutputDirectory = new File(project.xcodebuild.symRoot, project.xcodebuild.configuration + "-" + project.xcodebuild.sdk)
		def fileList = buildOutputDirectory.list(
						[accept: {d, f -> f ==~ /.*app/ }] as FilenameFilter
		).toList()
		if (fileList.size() == 0) {
			throw new IllegalStateException("No App Found in directory " + buildOutputDirectory.absolutePath)
		}
		def appName = buildOutputDirectory.absolutePath + "/" + fileList[0]
		def ipaName = appName.substring(0, appName.size()-4) + ".ipa"
		println "Signing " + appName + " to create " + ipaName




		def commandList = [
						"xcrun",
						"-sdk",
						project.xcodebuild.sdk,
						"PackageApplication",
						"-v",
						appName,
						"-o",
						ipaName
		]

		if (project.xcodebuild.signing.identity != null && project.xcodebuild.signing.mobileProvisionFile != null) {
			commandList.add("--sign");
			commandList.add(project.xcodebuild.signing.identity)
			commandList.add("--embed");
			commandList.add(project.xcodebuild.signing.mobileProvisionFile.absolutePath)
		}

/*
        if [ ! $CODESIGN_ALLOCATE ]
        then
        export CODESIGN_ALLOCATE=$(xcrun -find codesign_allocate)
        fi
        */

		def codesignAllocateCommand = runCommandWithResult(["xcrun", "-find", "codesign_allocate"]).trim();
		def environment = [CODESIGN_ALLOCATE:codesignAllocateCommand]
		runCommand(".", commandList, environment)
	}

}
