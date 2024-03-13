set PRETTY=1
REM # If PRETTY=1 then the response is formatted nicely, omit or set PRETTY to 0 for raw JSON
echo Getting last 3 messages - WITHOUT metadata
curl  -d "channel=%SLACK_CHANNEL_ID%" -d "limit=3" -d "include_all_metadata=false" -d "pretty=%PRETTY%" -H "Authorization: Bearer %SLACK_TOKEN%" -X POST https://slack.com/api/conversations.history
echo ------------------------------------------------
echo Getting last 3 messages - with metadata
curl  -d "channel=%SLACK_CHANNEL_ID%" -d "limit=3" -d "pretty=%PRETTY%" -H "Authorization: Bearer %SLACK_TOKEN%" -X POST https://slack.com/api/conversations.history

