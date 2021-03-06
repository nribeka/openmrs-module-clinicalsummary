/**
 * The contents of this file are subject to the OpenMRS Public License
 * Version 1.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://license.openmrs.org
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific language governing rights and limitations
 * under the License.
 *
 * Copyright (C) OpenMRS, LLC.  All Rights Reserved.
 */

package org.openmrs.module.clinicalsummary.web.controller.evaluator;

import javax.servlet.http.HttpSession;
import java.util.Date;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.Cohort;
import org.openmrs.Location;
import org.openmrs.Patient;
import org.openmrs.api.context.Context;
import org.openmrs.module.clinicalsummary.Index;
import org.openmrs.module.clinicalsummary.Summary;
import org.openmrs.module.clinicalsummary.rule.ResultCacheInstance;
import org.openmrs.module.clinicalsummary.service.CoreService;
import org.openmrs.module.clinicalsummary.service.IndexService;
import org.openmrs.module.clinicalsummary.service.SummaryService;
import org.openmrs.module.clinicalsummary.web.controller.WebUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping(value = "/module/clinicalsummary/evaluator/evaluateRule")
public class EvaluateRuleController {

    private static final Log log = LogFactory.getLog(EvaluateRuleController.class);

    @RequestMapping(method = RequestMethod.GET)
    public void populatePage(final ModelMap map) {
    }

    @RequestMapping(method = RequestMethod.POST)
    public String processForm(final @RequestParam(required = false, value = "locationId") String locationId,
                              final @RequestParam(required = false, value = "obsStartDate") Date startDate,
                              final @RequestParam(required = false, value = "obsEndDate") Date endDate,
                              final HttpSession session) {

        int maxInactiveInterval = session.getMaxInactiveInterval();
        session.setMaxInactiveInterval(-1);
        Location location = Context.getLocationService().getLocation(NumberUtils.toInt(locationId, -1));
        Cohort cohort = Context.getService(CoreService.class).getDateCreatedCohort(location, startDate, endDate);

        Integer counter = 0;
        int firstElement = 0;
        for (Integer patientId : cohort.getMemberIds()) {
            Patient patient = Context.getPatientService().getPatient(patientId);
            SummaryService service = Context.getService(SummaryService.class);
            // reuse index that's already exists but not needed anymore
            List<Index> activeIndexes = Context.getService(IndexService.class).getIndexes(patient);
            List<Summary> summaries = service.getSummaries(patient);
            while (!activeIndexes.isEmpty()) {
                Index activeIndex = activeIndexes.remove(firstElement);
                if (CollectionUtils.isNotEmpty(summaries) && !summaries.contains(activeIndex.getSummary()))
                    Context.getService(IndexService.class).deleteIndex(activeIndex);
            }
            counter++;
            if (counter % 20 == 0) {
                Context.flushSession();
                Context.clearSession();
            }
            ResultCacheInstance.getInstance().clearCache(patient);
        }
        session.setMaxInactiveInterval(maxInactiveInterval);
        return "redirect:evaluateRule.form";
    }

}
