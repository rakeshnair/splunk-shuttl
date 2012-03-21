// Copyright (C) 2011 Splunk Inc.
//
// Splunk Inc. licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
package com.splunk.shep.archiver.thaw;

import java.io.File;

import com.splunk.Service;

/**
 * Gets settings from the configured Splunk.
 */
public class SplunkSettings {

    private final Service splunkService;

    /**
     * @param splunkService
     */
    public SplunkSettings(Service splunkService) {
	this.splunkService = splunkService;
    }

    /**
     * @return thaw location for specified index.
     */
    public File getThawLocation(String index) {
	return new File(splunkService.getIndexes().get(index)
		.getThawedPathExpanded());
    }

}
