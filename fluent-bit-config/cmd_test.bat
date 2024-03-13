  SET "_test=%time%%"
  SET "_result=%_test::=_%"
  SET "_result=%_result: =_%"
  echo %time%  %RANDOM% %USERNAME% > out_%_result%.txt
