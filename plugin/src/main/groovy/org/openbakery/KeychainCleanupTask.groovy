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

import org.gradle.api.tasks.TaskAction

class KeychainCleanupTask extends AbstractXcodeTask {

	KeychainCleanupTask() {
		super()
		this.description = "Cleanup the keychain"
	}

	@TaskAction
	def clean() {
		if (project.xcodebuild.signing.keychain) {
			println "Nothing to cleanup"
			return;
		}


		project.xcodebuild.signing.keychainDestinationRoot.deleteDir();

		String result = runCommandWithResult(["security", "list"])
		String[] keychains = result.split("\n")
		for (String keychain : keychains) {
			def matcher = keychain =~ /^\s*"(.*)"$/
			File keychainFile = new File(matcher[0][1])
			if (!keychainFile.exists()) {
				if (keychainFile.name.startsWith(XcodeBuildPluginExtension.KEYCHAIN_NAME_BASE)) {
					println "deleting keychain: " + keychainFile
					try {
					runCommand(["security", "delete-keychain", keychainFile.absolutePath])
					} catch (IllegalStateException ex) {
						// ignore because delete-keychain results in an error because the file does not exists
						// but the entry is deleted properly
					}
				} else {
					println "keychain was not created by this plugin so leave it: " + keychainFile
				}

			} else if (keychainFile.name.equals("gradle.keychain")) {
				// gradle.keychain is the xcodelugin version 0.7 keychain that also needs to be cleaned
				println "deleting old 0.7 xcodeplugin keychain file"
				runCommand(["security", "delete-keychain", keychainFile.absolutePath])
			} else {
				println "keychain exists so leave it: " + keychainFile
			}

		}
	}

}