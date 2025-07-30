# Releasing the WildFly AI Feature Pack

* `mvn release:prepare -DpushChanges=false -DpreparationGoals="clean install"`
* Deploy in nexus staging repository
** `mvn release:perform -DlocalCheckout=true -Pjboss-release,jboss-staging-deploy`
* Check that all is correct in https://repository.jboss.org/nexus/#browse/browse:wildfly-extras-staging
* Deploy to nexus release repository
** `mvn -Pjboss-staging-move nxrm3:staging-move`
*Update githug
** `git push origin`
** `git push origin --tags`
* DONE
