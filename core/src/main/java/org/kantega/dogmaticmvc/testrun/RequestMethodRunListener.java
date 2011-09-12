/*
 * Copyright 2011 Kantega AS
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.kantega.dogmaticmvc.testrun;

import org.junit.runner.Description;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;

import java.util.ArrayList;
import java.util.List;

/**
*
*/
public class RequestMethodRunListener extends RunListener {
    private List<Description> ignoredTests = new ArrayList<Description>();
    private List<Failure> assumptionFailures = new ArrayList<Failure>();
    private List<Failure> testFailures = new ArrayList<Failure>();

    @Override
    public void testIgnored(Description description) throws Exception {
        ignoredTests.add(description);
    }

    @Override
    public void testAssumptionFailure(Failure failure) {
        assumptionFailures.add(failure);

    }

    @Override
    public void testFailure(Failure failure) throws Exception {
        testFailures.add(failure);
    }

    @Override
    public void testFinished(Description description) throws Exception {

    }

    @Override
    public void testStarted(Description description) throws Exception {

    }

    @Override
    public void testRunFinished(Result result) throws Exception {

    }

    @Override
    public void testRunStarted(Description description) throws Exception {

    }

    public List<Failure> getTestFailures() {
        return testFailures;
    }
}
