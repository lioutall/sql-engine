## testIf
> 注释:测试If
```sql
select *
from tab
where a = #a#
:if b isNotNull::
and b = #b#
:else
and b != #b#
if:
```

## testFor
> 测试For
```sql
select *
from tab
where a = #a#
and b in (
:for l:list :with ,::
#l#
for:
)
```

## testSubMain
> 调用上面的代码块
```sql
select * from tab where 1=1
:sub z.testSub sub:
```

## testNesting
> 测试嵌套
```sql
select * from tab where 1=1
:if b='bb' ::
and b = #b#
:if c='c' ::
and b = #c#
if:
if:

```

## testBug
> 测试bug
```sql
up a
set("a", b, c) = (#a#, #b#, #c#)
where d=#a#
```