/*
 * Copyright (C) 2016 Yong Zhu.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at http://www.apache.org/licenses/LICENSE-2.0 Unless required by
 * applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS
 * OF ANY KIND, either express or implied. See the License for the specific
 * language governing permissions and limitations under the License.
 */
package com.tollge.sql.template.jexparser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ExParser parse an expression, return type can be Boolean, String, Long,
 * Double, null
 * 
 * Here the parse method only support very few keywords: * > < = >= <= + - /
 * equals equalsIgnoreCase contains containsIgnoreCase startWith
 * startWithIgnoreCase endWith endWithIgnoreCase or and not ' () ? 0~9
 * 
 * Usage:
 * 
 * <pre>
 * Map<String, Object> keywords = new HashMap<String, Object>();
 * keywords.put("USERNAME", "Tom");
 * keywords.put("ID", "001");
 * Assert.assertEquals(true, new ExParser().doParse(keywords,
 * 		"(1+2)*3/4>0.1/(9+?) and (userName equals ?) or id equals ?", 100, "Tom", "001"));
 * 
 * or:
 * ExParser parser=new ExParser();
 * ExpItem[] expItems = parser.compile(
 *			"(1+2)*3/4>0.1/(9+?) and (userName equals ?) and id equals ? and (200 = ? ) and (FOO = 123 or BAR equals '456')");
 * Assert.assertEquals(true, parser.doParse(expItems, keywords, 100, "Tom", "001", 200));
 * 
 * </pre>
 * 
 * @author Yong Zhu (Yong9981@gmail.com)
 * @since 1.0.0
 */
public class ExParser {
	/**
	 * Registered functions, key is function name, value is function priority
	 */
	static Map<String, Integer> functionMap = new HashMap<String, Integer>();

	static {
		functionMap.put("*", 2);
		functionMap.put("/", 2);
		functionMap.put("+", 4);
		functionMap.put("-", 4);
		functionMap.put("EQUALS", 6);
		functionMap.put("EQUALSIGNORECASE", 6);
		functionMap.put("CONTAINS", 6);
		functionMap.put("CONTAINSIGNORECASE", 6);
		functionMap.put("STARTWITH", 6);
		functionMap.put("STARTWITHIGNORECASE", 6);
		functionMap.put("ENDWITH", 6);
		functionMap.put("ENDWITHIGNORECASE", 6);
		functionMap.put("IS", 6);
		functionMap.put(">", 8);
		functionMap.put("<", 8);
		functionMap.put("=", 8);
		functionMap.put(">=", 8);
		functionMap.put("<=", 8);
		functionMap.put("<>", 8);
		functionMap.put("NOT", 10);
		functionMap.put("AND", 12);
		functionMap.put("OR", 14);
	}

	/**
	 * Item type can be: <br/>
	 * S:String, B:Boolean, L: Long, D:Double, F:function, N:Null, (:sub items,
	 * U:Unknow(need correct), P:parameter(need correct)
	 * 
	 * @author Yong Zhu
	 * @since 1.7.0
	 */
	public static class ExpItem {
		public char type;
		public int priority;
		public Object value;

		ExpItem[] subItems;

		void setTypeAndValue(char type, Object value) {
			this.type = type;
			this.value = value;
		}

		ExpItem cloneItem() {
			ExpItem newItem = new ExpItem();
			newItem.priority = priority;
			newItem.type = type;
			newItem.value = value;
			if (subItems != null) {
				ExpItem[] newSubItems = new ExpItem[subItems.length];
				for (int i = 0; i < subItems.length; i++)
					newSubItems[i] = subItems[i].cloneItem();
				newItem.subItems = newSubItems;
			}
			return newItem;
		} 

		void guess(Object obj) {
			value = obj;
			if (obj == null)
				type = 'N';
			else if (obj instanceof String)
				type = 'S';
			else if (obj instanceof Boolean)
				type = 'B';
			else if (obj instanceof Long)
				type = 'L';
			else if (obj instanceof Integer) {
				type = 'L';
				value = (long) (Integer) value;// NOSONAR
			} else if (obj instanceof Byte) {
				type = 'L';
				value = (long) (Byte) value;// NOSONAR
			} else if (obj instanceof Double)
				type = 'D';
			else if (obj instanceof Float) {
				type = 'D';
				value = (double) (Float) value;// NOSONAR
			} else
				throw new ExParserException("Unrecognized expression data type for '" + obj + "'");
		}

	}

	static class SearchResult {
		ExpItem item;
		int leftStart;
		int leftEnd;

		SearchResult(ExpItem item, int leftStart, int leftEnd) {
			this.item = item;
			this.leftStart = leftStart;
			this.leftEnd = leftEnd;
		}
	}

	static class ParamPosition {
		int position = 0;
	}

	/**
	 * Parse a expression String, return an object result
	 * 
	 * @param bean Expression allow direct use only 1 bean's fields
	 * @param keyWords The preset key words key-value map
	 * @param expression The expression
	 * @param params The expression parameter array
	 * @return an object result
	 */
	public Object doParse(Map<String, Object> keyWords, String expression, Object... params) {
		if (ExParserUtils.isEmpty(expression))
			return null;
		char[] chars = (" " + expression + " ").toCharArray();
		ExpItem[] items = seperateCharsToItems(chars, 1, chars.length - 2);
		for (ExpItem item : items) {
			correctType(item);
		}
		ParamPosition paramPosition = new ParamPosition();
		for (ExpItem item : items) {
			correctKeywordAndParam(item, keyWords, paramPosition, params);
		}
		ExpItem item = calculate(items);
		return item.value;
	}

	/** Compile a String expression to ExpItem array */
	public ExpItem[] compile(String expression) {
		if (ExParserUtils.isEmpty(expression))
			return null;
		char[] chars = (" " + expression + " ").toCharArray();
		ExpItem[] items = seperateCharsToItems(chars, 1, chars.length - 2);
		for (ExpItem item : items)
			correctType(item); 
		return items;
	}

	/**
	 * Parse a complied expItem list, return an object result
	 * 
	 * @param items The compiled ExpItem list
	 * @param keyWords The preset key words key-value map
	 * @param params The expression parameter array
	 * @return an object result
	 */
	public Object doParse(ExpItem[] items, Map<String, Object> keyWords, Object... params) {
		ExpItem[] newItems = new ExpItem[items.length];
		for (int i = 0; i < items.length; i++)
			newItems[i] = items[i].cloneItem();
		ParamPosition paramPosition = new ParamPosition();
		for (ExpItem item : newItems)
			correctKeywordAndParam(item, keyWords, paramPosition, params);
		ExpItem item = calculate(newItems);
		return item.value;
	}

	/** Separate chars to Items list */
	ExpItem[] seperateCharsToItems(char[] chars, int start, int end) {
		List<ExpItem> items = new ArrayList<ExpItem>();
		SearchResult result = findFirstResult(chars, start, end);
		while (result != null) {
			items.add(result.item);
			result = findFirstResult(chars, result.leftStart, result.leftEnd);
		}
		return items.toArray(new ExpItem[items.size()]);
	}

	/** if is U type, use this method to correct type */
	void correctType(ExpItem item) {
		if (item.type == 'U') {// correct Unknown type to other type
			String valueStr = (String) item.value;
			String valueUpcase = valueStr.toUpperCase();
			// check is function
			if (functionMap.containsKey(valueUpcase)) {
				item.type = 'F';
				item.value = valueUpcase;
				item.priority = functionMap.get(valueUpcase);
			}
			if (item.type == 'U')// still not found
				try { // is Long able?
					item.setTypeAndValue('L', Long.parseLong(valueUpcase));
				} catch (NumberFormatException e) {
					try {// is Double able?
						item.setTypeAndValue('D', Double.parseDouble(valueUpcase));
					} catch (NumberFormatException e1) { // is Boolean able?
						if ("TRUE".equalsIgnoreCase(valueUpcase)) {
							item.setTypeAndValue('B', true);
						} else if ("FALSE".equalsIgnoreCase(valueUpcase)) {
							item.setTypeAndValue('B', false);
						} else if ("NULL".equalsIgnoreCase(valueUpcase)) {
							item.setTypeAndValue('N', null);
						}
					}
				}
		}
		if (item.subItems != null)
			for (ExpItem t : item.subItems)
				correctType(t);
	}

	/** if is U type, use this method to correct type */
	void correctKeywordAndParam(ExpItem item, Map<String, Object> presetValues, ParamPosition paramPostion,
			Object... params) {
		if (item.type == 'P') {
			item.guess(params[paramPostion.position++]);
		} else if (item.type == 'U') {// correct Unknown type to other type
			String valueUpcase = ((String) item.value);
			if (presetValues != null && presetValues.containsKey(valueUpcase))
				item.guess(presetValues.get(valueUpcase));
			if (item.type == 'U')// still not found
				throw new ExParserException("Unrecognized expression near '" + valueUpcase + "'");
		}
		if (item.subItems != null)
			for (ExpItem t : item.subItems)
				correctKeywordAndParam(t, presetValues, paramPostion, params);
	}

	/**
	 * Find first item and store left start and left end position in SearchResult
	 */
	SearchResult findFirstResult(char[] chars, int start, int end) {
		if (start > end)
			return null;
		boolean letters = false;
		StringBuilder sb = new StringBuilder();
		for (int i = start; i <= end; i++) {
			if (!letters) {// no letters found
				if (chars[i] == '?') {
					ExpItem item = new ExpItem();
					item.type = 'P';
					item.value = "?";
					return new SearchResult(item, i + 1, end);
				} else if (chars[i] == '\'') {
					for (int j = i + 1; j <= end; j++) {
						if (chars[j] == '\'' && chars[j - 1] != '\\') {
							ExpItem item = new ExpItem();
							item.type = 'S';
							item.value = sb.toString();
							return new SearchResult(item, j + 1, end);
						} else
							sb.append(chars[j]);
					}
					throw new ExParserException("Miss right ' charactor in expression.");
				} else if (chars[i] == '(') {
					int count = 1;
					boolean inString = false;
					for (int j = i + 1; j <= end; j++) {
						if (!inString) {
							if (chars[j] == '(')
								count++;
							else if (chars[j] == ')') {
								count--;
								if (count == 0) {
									ExpItem[] subItems = seperateCharsToItems(chars, i + 1, j - 1);
									ExpItem item = new ExpItem();
									item.type = '(';
									item.subItems = subItems;
									return new SearchResult(item, j + 1, end);
								}
							} else if (chars[j] == '\'') {
								inString = true;
							}
						} else {
							if (chars[j] == '\'' && chars[j - 1] != '\\') {
								inString = false;
							}
						}
					}
					throw new ExParserException("Miss right ) charactor in expression.");
				} else if (chars[i] > ' ') {
					letters = true;
					sb.append(chars[i]);
				}
			} else {// letters found
				if (chars[i] == '?' || chars[i] == '\'' || chars[i] == '(' || chars[i] <= ' '
						|| ExParserUtils.isLetterNumber(chars[i]) != ExParserUtils.isLetterNumber(chars[i - 1])) {
					ExpItem item = new ExpItem();
					item.type = 'U';
					item.value = sb.toString();
					return new SearchResult(item, i, end);
				} else {
					sb.append(chars[i]);
				}
			}
		}
		if (sb.length() > 0) {
			ExpItem item = new ExpItem();
			item.type = 'U';
			item.value = sb.toString();
			return new SearchResult(item, end + 1, end);
		} else
			return null;
	}

	/** Calculate items list into one item */
	ExpItem calculate(ExpItem[] items) {
		for (ExpItem item : items) {
			if (item.subItems != null) {
				ExpItem newSubItem = calculate(item.subItems);
				item.type = newSubItem.type;
				item.value = newSubItem.value;
				item.subItems = null;
			}
		} // now there is no subItems

		// find highest priority function
		int functionPos;
		functionPos = -1;
		int priority = 100;
		for (int i = 0; i < items.length; i++) {
			if (items[i].type == 'F' && items[i].priority < priority) {
				functionPos = i;
				priority = items[i].priority;
			}
		}
		while (functionPos != -1) {
			doCalculate(items, functionPos);
			functionPos = -1;
			priority = 100;
			for (int i = 0; i < items.length; i++) {
				if (items[i].type == 'F' && items[i].priority < priority) {
					functionPos = i;
					priority = items[i].priority;
				}
			}
		} // until all function be calculated

		int found = 0;
		ExpItem result = null;
		for (ExpItem item : items) {
			if (item.type != '0') {
				result = item;
				if (++found > 1)
					throw new ExParserException("More than 1  calculated result found");
			}
		}
		if (result == null) {
			result = new ExpItem();
			result.type = 'N';
		}
		return result;

	}

	/** Execute the function and mark some items to '0' means deleted it */
	void doCalculate(ExpItem[] items, int functionPos) {
		ExpItem lastItem = null;
		ExpItem nextItem = null;
		for (int i = functionPos - 1; i >= 0; i--) {
			if (items[i].type != '0') {
				lastItem = items[i];
				break;
			}
		}
		for (int i = functionPos + 1; i < items.length; i++) {
			if (items[i].type != '0') {
				nextItem = items[i];
				break;
			}
		}
		ExParserUtils.doTheMath(items[functionPos], lastItem, nextItem);
	}

}