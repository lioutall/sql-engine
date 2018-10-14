# sql-engine
Simple sql template engine, very small, no need to introduce external packages (you can change a few lines of code without introducing any external packages).

#### sql template
```
select *
from tab
where a = #a#
?if b='bb' ??
and b = #b#
?else
and b != #b#
if?
```
**java code**
```
Map<String, Object> params = new HashMap<>();
params.put("a", "a");
params.put("b", "bbb");
SqlTemplate.generateSQL("test.testIf", params)
```
#### output:
> sql:  select * from tab where a = ? and b != ?   
params:  [a, bbb]


## grammar
### IF
> The conditional rules of if will be discussed later
```
?if a==1 ??
  mix
  if?
```
### IF_ELSE
```
?if a==1 ??
  mix
  ?else
  mix
  if?
```
### FOR
```
?for a?as ??
  mix
  for
```
### FOR_WITH
> b is string
```
?for a?as ?with b ??
  mix
  for?
```
### SUB
> sub template
```
?sub test.testSub sub?
```
## IF condition
IF Conditional engine is open source[jexparser](https://gitee.com/drinkjava2/jexparser)   
it contains 3 classes, just copy into the jar.

Currently supported:
```
>  <  =  >=  <=  
+  -  *  /  
or  and  not  
'  ( )  ?  0~9 . 
equals  equalsIgnoreCase  contains  containsIgnoreCase  
startWith  startWithIgnoreCase  endWith  endWithIgnoreCase
```
Added a keyword based on it: isNotNull

### User Guide
sql template folder: mapper   
sql template file type: .md  (yeah, it is markdown)   
Template file composition:
```
## testIf      ->   key of sql
> Comment:test If   -> Comment
```sql         -> sql content

```sub         -> sub template,Can only be referenced by sql template

```
When the SqlTemplate class is initialized, all sql templates are loaded and saved as xxx.testIf, xxx is the file name, and testIf is the sql key.
You need to call:
```
SqlTemplate.generateSQL("xxx.testIf", params)
```


