# IUSG Congress Bill Dashboarding Site

## How to check out and run
1. Make sure IntelliJ IDEA, Git, and Oracle JDK 8/OpenJDK 8 are installed
2. Make sure Docker for Windows/Docker for Mac (or Linux) is installed. If you do not have Windows 10 Pro,
you need to use IUWare to get a Windows EDU activation key and use it in your activation settings.
3. Make sure Linux containers are being used.
4. Run the following command to bring up a local database:
`docker run -d --name iusg-db -p 8080:8080 -p 8081:8081 -p 28015:28015 -p 29015:29015 rethinkdb`
5. Set environment variables
    - Click the green run button on the main method, click on "CongressDashboardKt" configuration.
    - `database_hostname` - localhost if running locally
    - `url_base` - localhost if running locally
    - `cleanse` - whether to remove all data first
    - `dbpassword` - rethinkdb password if running in prod or staging




### Messages:
Student life desc: The Student Life Committee is responsible for issues to pertaining to campus safety, issues of general health and well‐being in and outside the campus community, and initiatives of a recreational nature intended to improve the student experience.  As a standing committee, the Student Life Committee will have the power to adopt resolutions through the sponsorship of one or more of its members for initiatives that relate to its committees.

Steering desc: The Congressional Steering Committee is composed of all five chairpersons of the IUSG standing committees.  The Speaker of the Congress chairs the committee and is the last committee member to vote. The Committee acts as a medium of information exchange between the Congress and the Congressional Secretary and serves as an informational source for Congress Members concerning executive matters. Resolutions of Reprimand or Censure shall be adopted by the Committee. Upon accusations of violations of the IUSG Code of Conduct, the Congressional Steering Committee shall serve as a conduct committee, as regulated by Article XIV of these bylaws.

Env affairs desc: The Environmental Affairs Committee shall be responsible for issues pertaining to the practices of conservation and  responsibility for the environment. As a standing committee, the Environmental Affairs committee will have the   power to adopt resolutions through the sponsorship of one or more of its members for initiatives that relate to   its committees.

