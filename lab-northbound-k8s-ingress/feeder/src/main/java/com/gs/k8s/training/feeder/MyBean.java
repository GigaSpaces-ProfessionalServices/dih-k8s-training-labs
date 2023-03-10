/*
 * Copyright (c) 2008-2016, GigaSpaces Technologies, Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.gs.k8s.training.feeder;

import org.slf4j.*;
import javax.annotation.*;

import org.openspaces.core.*;
import org.openspaces.core.space.*;
import com.gs.k8s.training.common.Employee;

import java.time.LocalDate;

public class MyBean {
    private static final Logger logger = LoggerFactory.getLogger(MyBean.class);

    @Resource
    private GigaSpace gigaSpace;

    @PostConstruct
    public void initialize() {
        logger.info("Initialized: connected to space {}", gigaSpace.getSpaceName());
        // Your code goes here, for example:

        Employee employee1 = new Employee();
        employee1.setId(1);
        employee1.setName("Rajiv Shah");
        employee1.setTitle("Professional Services");
        employee1.setDepartment("Sales");
        employee1.setSalary(100000);
        employee1.setBirthday(LocalDate.of(2023, 1, 26));
        gigaSpace.write(employee1);

        Employee employee2 = new Employee();
        employee2.setId(2);
        employee2.setDepartment("Engineering");
        employee2.setTitle("Customer Service Manager");
        employee2.setSalary(100000);
        employee2.setName("Aharon Moll");
        employee2.setBirthday(LocalDate.of(2023, 1, 27));
        gigaSpace.write(employee2);

        logger.info("Finished writing objects to space.");
    }

    @PreDestroy
    public void close() {
        logger.info("Closing");
    }
}
