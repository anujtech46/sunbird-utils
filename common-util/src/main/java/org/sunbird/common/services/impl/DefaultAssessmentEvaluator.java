/** */
package org.sunbird.common.services.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.ProjectUtil;
import org.sunbird.common.services.AssessmentEvaluator;

/** @author Manzarul This class will do the default assessment evaluation logic. */
public class DefaultAssessmentEvaluator implements AssessmentEvaluator {

  /** this method can evaluate assessment for multiple user at a time */
  @Override
  public Map<String, List<Map<String, Object>>> evaluateAssessment(
      Map<String, List<Map<String, Object>>> data) {
    Map<String, List<Map<String, Object>>> response =
        new HashMap<String, List<Map<String, Object>>>();
    Map<String, List<Map<String, Object>>> request = data;
    if (request.size() > 0)
      for (Entry<String, List<Map<String, Object>>> entry : request.entrySet()) {
        if (entry.getValue().size() > 0) {
          List<Map<String, Object>> assessmentList = getAssessmentListWithMaxScore(entry);
          response.put(entry.getKey(), assessmentList);
        }
      }

    return response;
  }

  /**
   * this method will evaluate result of an user at a time
   *
   * @param evaluatedData Map<String, List<Map<String, Object>>> (map of userId, list of assessment
   *     obj)
   * @return List<Map<String, Object>>
   */
  @Override
  public List<Map<String, Object>> evaluateResult(
      Map<String, List<Map<String, Object>>> evaluatedData) {
    List<List<Map<String, Object>>> assmntList =
        new ArrayList<List<Map<String, Object>>>(evaluatedData.values());
    List<Map<String, Object>> request = assmntList.get(0);
    Map<String, List<Map<String, Object>>> assmntMap = new HashMap<>();
    String tempKey =
        (String) request.get(0).get(JsonKey.COURSE_ID)
            + "#"
            + (String) request.get(0).get(JsonKey.CONTENT_ID);
    List<Map<String, Object>> list = new ArrayList<>();
    list.add(request.get(0));
    assmntMap.put(tempKey, list);
    String contentId = null;
    String courseId = null;
    for (int i = 1; i < request.size(); i++) {
      contentId = (String) request.get(i).get(JsonKey.CONTENT_ID);
      courseId = (String) request.get(i).get(JsonKey.COURSE_ID);
      tempKey = courseId + "#" + contentId;
      if (assmntMap.containsKey(tempKey)) {
        assmntMap.get(tempKey).add(request.get(i));
      } else {
        list = new ArrayList<>();
        list.add(request.get(i));
        assmntMap.put(tempKey, list);
      }
    }

    return evaluateResultAndGrade(assmntMap);
  }

  private List<Map<String, Object>> evaluateResultAndGrade(
      Map<String, List<Map<String, Object>>> assmntMap) {
    double score = 0;
    double maxScore = 0;
    String userId = "";
    String courseId = "";
    String contentId = "";
    Map<String, Object> resultMap = null;
    List<Map<String, Object>> listMap = new ArrayList<>();
    if (assmntMap != null && assmntMap.size() > 0) {
      Iterator<Entry<String, List<Map<String, Object>>>> itr = assmntMap.entrySet().iterator();
      while (itr.hasNext()) {
        Entry<String, List<Map<String, Object>>> entry = itr.next();
        List<Map<String, Object>> list = entry.getValue();

        for (Map<String, Object> map : list) {
          courseId = (String) map.get(JsonKey.COURSE_ID);
          contentId = (String) map.get(JsonKey.CONTENT_ID);
          userId = (String) map.get(JsonKey.USER_ID);
          score =
              score
                  + (((String) map.get(JsonKey.ASSESSMENT_SCORE)) != null
                      ? Double.parseDouble((String) map.get(JsonKey.ASSESSMENT_SCORE))
                      : 0);
          maxScore =
              maxScore
                  + (((String) map.get(JsonKey.ASSESSMENT_MAX_SCORE)) != null
                      ? Double.parseDouble((String) map.get(JsonKey.ASSESSMENT_MAX_SCORE))
                      : 0);
        }
        double percentage = ProjectUtil.calculatePercentage(score, maxScore);
        ProjectUtil.AssessmentResult result = ProjectUtil.calcualteAssessmentResult(percentage);
        resultMap = new HashMap<>();
        resultMap.put(JsonKey.USER_ID, userId);
        resultMap.put(JsonKey.COURSE_ID, courseId);
        resultMap.put(JsonKey.CONTENT_ID, contentId);
        resultMap.put(JsonKey.RESULT, result.getResult());
        resultMap.put(JsonKey.ASSESSMENT_GRADE, result.getGrade());
        resultMap.put(JsonKey.ASSESSMENT_SCORE, String.valueOf(score));
        listMap.add(resultMap);
        contentId = "";
        userId = "";
        contentId = "";
        score = 0;
        maxScore = 0;
      }
    }

    return listMap;
  }

  private List<Map<String, Object>> getAssessmentListWithMaxScore(
      Entry<String, List<Map<String, Object>>> entry) {
    List<Map<String, Object>> request = entry.getValue();
    Map<String, Map<String, Object>> assessmentWithMaxScore = new HashMap<>();
    String tempKey =
        (String) request.get(0).get(JsonKey.COURSE_ID)
            + "#"
            + (String) request.get(0).get(JsonKey.CONTENT_ID)
            + "#"
            + (String) request.get(0).get(JsonKey.ASSESSMENT_ITEM_ID);
    assessmentWithMaxScore.put(tempKey, request.get(0));
    String assessmentItemId = null;
    String contentId = null;
    String courseId = null;
    Double score = null;
    if (request.size() > 1) {
      for (int i = 1; i < request.size(); i++) {
        assessmentItemId = (String) request.get(i).get(JsonKey.ASSESSMENT_ITEM_ID);
        contentId = (String) request.get(i).get(JsonKey.CONTENT_ID);
        courseId = (String) request.get(i).get(JsonKey.COURSE_ID);
        tempKey = courseId + "#" + contentId + "#" + assessmentItemId;
        score =
            (request.get(i).get(JsonKey.ASSESSMENT_SCORE)) != null
                ? Double.parseDouble((String) request.get(i).get(JsonKey.ASSESSMENT_SCORE))
                : 0;
        if (assessmentWithMaxScore.containsKey(tempKey)) {
          Map<String, Object> tempMap = (Map<String, Object>) assessmentWithMaxScore.get(tempKey);
          if (null != tempMap.get(JsonKey.ASSESSMENT_SCORE)
              && Double.compare(
                      score, Double.parseDouble((String) tempMap.get(JsonKey.ASSESSMENT_SCORE)))
                  > 0) {
            assessmentWithMaxScore.put(tempKey, request.get(i));
          }
        } else {
          assessmentWithMaxScore.put(tempKey, request.get(i));
        }
      }
    }
    return new ArrayList<Map<String, Object>>(assessmentWithMaxScore.values());
  }
}
