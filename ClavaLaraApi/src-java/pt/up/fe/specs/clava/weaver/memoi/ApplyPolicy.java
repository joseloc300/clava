package pt.up.fe.specs.clava.weaver.memoi;
import java.util.List;
import java.util.function.Predicate;

/**
 * Copyright 2019 SPeCS.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License. under the License.
 */

public class ApplyPolicy {

    public static final Predicate<MergedMemoiReport> IF_NOT_EMPTY = (r) -> {

        List<Integer> reportElements = r.getElements();

        /*
        int reportCount = r.getReportCount();
        
        double mean = reportElements.stream().reduce(0, Integer::sum) / reportCount;
        
        return mean != 0.0;
        
        */
        return reportElements.stream().anyMatch(e -> e != 0.0);
    };
}
