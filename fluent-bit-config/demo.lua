--[[ Determine which OS we're running on--]]
function getOS()
  local osname
  -- ask LuaJIT first
  if jit then
    return jit.os
  end
  -- Unix, Linux variants
  local fh, err = assert(io.popen("uname -o 2>/dev/null", "r"))
  if fh then
    osname = fh:read()
  end

  return osname or "Windows"
end

--[[ Pretty printer util for outputting the payload - here to help with diagnostics if needed--]]
function printRecord(record)
  for key, value in pairs(record) do
    local elementType = type(value)
    if (elementType == "table") then
      print(string.format("%s { %s = ", key))
      printDetails(value .. " ")
      print("}")
    else
      print(string.format("%s %s = %s --> %s", "   ", key, tostring(value), elementType))
    end
  end
end

--[[ -This is the main function in the script and needs to be called. It retrieves from the
payload the cmd attribute which will tell us which script to invoke.
The script is assumed to be the cmd's value prefixed by cmd_ and post fixed with .sh or .bat
the OS also impacts how we call the script file.
The outcome of invoking the script is directed to remotecmd.lua.out
For diagnostics the printRecord method can be used
--]]
function cb_osCommand(tag, timestamp, record)
  local code = 0
  local commadAttribute = "command"
  local command = ""
  --[[printRecord(record)--]]
  printRecord(record)

  if (record[commadAttribute] ~= nil) then
    command = record[commadAttribute]

    print("Will execute " .. command)
    if (getOS() == "Windows") then
      command = "call cmd_" .. command .. ".bat"
    else
      command = "source cmd_" .. command .. ".sh"
    end
  else
    print("Lua no command identified")
  end

  local fullCommand = command .. " > remotecmd.lua.out"
  local runCommandResult = os.execute(fullCommand)
  print("response from exe command:" .. runCommandResult)
  return code, timestamp, record
end
