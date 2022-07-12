# ACTOR MODEL SYSTEM COMPRISING OF MASTER-CHILD TOPOLOGY

## Functionality
- Takes in data from a csv file
- Master extracts each record from the file and sends it to the child actors
- Routing mechanism used to send single record to a child at a time
- Supervidor Strategy implemented to take care of exceptions/ errors
- Actor implementation is in the style of classic and typed actors
- Actors are written in FSM format as well
- Graphs are written to act as a stream for the flow of data
- Error handling streams are also implemented
