## testIf
> 注释:测试If
```sql
select *
from tab
where a = #a#
?if b isNotNull??
and b = #b#
?else
and b != #b#
if?
```

## testFor
> 测试For
```sql
select *
from tab
where a = #a#
and b in (
?for l?list ?with ,??
#l#
for?
)
```

## testSubMain
> 调用上面的代码块
```sql
select * from tab where 1=1
?sub z.testSub sub?
```

## testNesting
> 测试嵌套
```sql
select * from tab where 1=1
?if b='bb' ??
and b = #b#
?if c='c' ??
and b = #c#
if?
if?

```


## dateformat
> 测试sql块
```sub
'yyyy-mm-dd hh24:mi:ss'
```

## testSome
> 测试某种
```sql
select now()
?if upName isNotNull??
and r1.user_name like '%' || #upName# || '%'
if?
?if a isNotNull??
and n.modify_time > to_timestamp(#a#, ?sub test.dateformat sub?)
if?

```
