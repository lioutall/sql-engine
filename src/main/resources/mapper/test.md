## testIf
> 注释:测试If
```sql
select *
from tab
where a = #a#
:if b='bb' ::
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

## testSub
> 测试sql块
```sub
:if b='bb' ::
and b = #b#
if:
:if c='c' ::
and c = #c#
if:
```

## testSubMain
> 调用上面的代码块
```sql
select * from tab where 1=1
:sub test.testSub sub:
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