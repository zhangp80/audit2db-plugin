/**
 *
 */
package org.jenkins.plugins.audit2db.test;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.jenkins.plugins.audit2db.data.BuildDetailsRepository;
import org.jenkins.plugins.audit2db.internal.data.AbstractHibernateRepository;
import org.jenkins.plugins.audit2db.internal.data.BuildDetailsHibernateRepository;
import org.jenkins.plugins.audit2db.internal.data.HibernateUtil;
import org.jenkins.plugins.audit2db.model.BuildDetails;
import org.jenkins.plugins.audit2db.model.BuildNode;
import org.jenkins.plugins.audit2db.model.BuildParameter;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.orm.hibernate3.HibernateTransactionManager;
import org.springframework.transaction.TransactionStatus;

/**
 * Unit tests for the {@link BuildDetailsHibernateRepository} class.
 *
 * @author Marco Scata
 *
 */
public class BuildDetailsHibernateRepositoryTests {
    private static final Logger LOGGER = Logger
    .getLogger(BuildDetailsHibernateRepositoryTests.class.getName());

    final String hostName = "MY_JENKINS";

    final BuildDetailsRepository repository = new BuildDetailsHibernateRepository(
	    HibernateUtil.getSessionFactory(HibernateUtil.getExtraProperties(
		    "org.hsqldb.jdbc.JDBCDriver", "jdbc:hsqldb:mem:test", "SA",
	    "")));

    final HibernateTransactionManager txmgr = ((BuildDetailsHibernateRepository) repository)
    .getTransactionManager();

    @Test
    public void createShouldReturnMatchingId() {
	final BuildDetails build = RepositoryTests.createRandomBuildDetails();
	final Object buildId = repository.saveBuildDetails(build);
	Assert.assertNotNull("Unexpected null build id", buildId);
	Assert.assertEquals("Unexpected build id", buildId, build.getId());
    }

    @Test
    public void createBuildsWithSameNodeShouldReuseNodeEntity() {
	final BuildDetails build1 = RepositoryTests.createRandomBuildDetails();
	build1.setId("BUILD_1");
	final BuildDetails build2 = RepositoryTests.createRandomBuildDetails();
	build2.setId("BUILD_2");

	repository.saveBuildDetails(build1);
	repository.saveBuildDetails(build2);

	final HibernateTemplate hibernate = new HibernateTemplate();
	hibernate.setSessionFactory(((AbstractHibernateRepository) repository)
		.getSessionFactory());

	final List<BuildNode> nodes = hibernate.loadAll(BuildNode.class);
	Assert.assertEquals("Unexpected number of node entities", 1,
		nodes.size());
    }

    @Test
    public void retrievingBuildNodeByNullUrlShouldThrowException() {
	try {
	    repository.getBuildNodeByUrl(null);
	    Assert.fail("Unexpected succesful retrieval of node with null URL");
	} catch (final Exception e) {
	    Assert.assertEquals("Unexpected exception type",
		    IllegalArgumentException.class, e.getClass());
	}
    }

    @Test
    public void retrievingBuildNodeByValidUrlShouldSucceed() {
	final BuildDetails build = RepositoryTests.createRandomBuildDetails();
	final Object buildId = repository.saveBuildDetails(build);
	Assert.assertNotNull("Unexpected null build id", buildId);

	final BuildNode expected = build.getNode();
	final BuildNode actual = repository
	.getBuildNodeByUrl(expected.getUrl());
	Assert.assertNotNull("Unexppected null build node", actual);
	Assert.assertEquals("Unexpected build node", expected, actual);
    }

    @Test
    public void retrievingBuildNodeByNonExistingUrlShouldReturnNull() {
	final BuildDetails build = RepositoryTests.createRandomBuildDetails();
	final Object buildId = repository.saveBuildDetails(build);
	Assert.assertNotNull("Unexpected null build id", buildId);

	final BuildNode actual = repository
	.getBuildNodeByUrl("NON_EXISTING_URL");
	Assert.assertNull("Unexppected non-null build node", actual);
    }

    @Test
    public void retrievalByNonMatchingIdShouldReturnNullEntity() {
	final BuildDetails build = RepositoryTests.createRandomBuildDetails();
	final Object buildId = repository.saveBuildDetails(build);
	Assert.assertNotNull("Unexpected null build id", buildId);

	final BuildDetails retrievedBuild = repository
	.getBuildDetailsById(build.getId() + "NOMATCH");
	Assert.assertNull("Unexpected null build", retrievedBuild);
    }

    @Test
    public void retrievalByMatchingIdShouldReturnSameEntity() {
	final BuildDetails build = RepositoryTests.createRandomBuildDetails();
	final Object buildId = repository.saveBuildDetails(build);
	Assert.assertNotNull("Unexpected null build id", buildId);

	final BuildDetails retrievedBuild = repository
	.getBuildDetailsById(build.getId());
	Assert.assertNotNull("Unexpected null build", build);
	Assert.assertEquals("Mismatching build details found", build,
		retrievedBuild);
    }

    @Test
    public void retrievalByNonMatchingDateRangeShouldReturnEmptyList() {
	final BuildDetails build = RepositoryTests.createRandomBuildDetails();
	final Object buildId = repository.saveBuildDetails(build);
	Assert.assertNotNull("Unexpected null build id", buildId);

	final Calendar start = Calendar.getInstance();
	start.set(1914, 6, 28, 9, 0, 0);
	final Calendar end = Calendar.getInstance();
	end.set(1918, 10, 11, 11, 0, 0);

	final List<BuildDetails> builds = repository
	.getBuildDetailsByDateRange(start.getTime(), end.getTime());
	Assert.assertNotNull("Unexpected null list of builds", builds);
	Assert.assertTrue("Unexpected non-empty list of builds",
		builds.isEmpty());
    }

    @Test
    public void retrievalByMatchingDateRangeShouldReturnNonEmptyList() {
	final BuildDetails build = RepositoryTests.createRandomBuildDetails();
	build.setEndDate(new Date());
	final Object buildId = repository.saveBuildDetails(build);
	Assert.assertNotNull("Unexpected null build id", buildId);

	final Calendar start = Calendar.getInstance();
	start.setTime(build.getStartDate());

	final Calendar end = Calendar.getInstance();
	end.setTime(build.getEndDate());

	List<BuildDetails> builds = repository.getBuildDetailsByDateRange(
		start.getTime(), end.getTime());
	Assert.assertNotNull("Unexpected null list of builds", builds);
	Assert.assertFalse("Unexpected empty list of builds", builds.isEmpty());
	Assert.assertEquals("Unexpected number of builds", 1, builds.size());
	Assert.assertEquals("Mismatching build details found", build,
		builds.get(0));

	start.setTime(new Date(build.getEndDate().getTime() - 10000));
	end.setTime(new Date(build.getEndDate().getTime() + 10000));

	builds = repository.getBuildDetailsByDateRange(start.getTime(),
		end.getTime());
	Assert.assertNotNull("Unexpected null list of builds", builds);
	Assert.assertFalse("Unexpected empty list of builds", builds.isEmpty());
	Assert.assertEquals("Unexpected number of builds", 1, builds.size());
    }

    @Test
    public void retrievalByNonMatchingDurationRangeShouldReturnEmptyList() {
	final BuildDetails build = RepositoryTests.createRandomBuildDetails();
	final Object buildId = repository.saveBuildDetails(build);
	Assert.assertNotNull("Unexpected null build id", buildId);

	final long min = build.getDuration() + 10;
	final long max = min + 100;

	final List<BuildDetails> builds = repository
	.getBuildDetailsByDurationRange(min, max);
	Assert.assertNotNull("Unexpected null list of builds", builds);
	Assert.assertTrue("Unexpected non-empty list of builds",
		builds.isEmpty());
    }

    @Test
    public void retrievalByMatchingDurationRangeShouldReturnNonEmptyList() {
	final BuildDetails build = RepositoryTests.createRandomBuildDetails();
	final Object buildId = repository.saveBuildDetails(build);
	Assert.assertNotNull("Unexpected null build id", buildId);

	final long min = build.getDuration() - 10;
	final long max = min + 100;

	final List<BuildDetails> builds = repository
	.getBuildDetailsByDurationRange(min, max);
	Assert.assertNotNull("Unexpected null list of builds", builds);
	Assert.assertFalse("Unexpected empty list of builds", builds.isEmpty());
	Assert.assertEquals("Unexpected number of builds", 1, builds.size());
	Assert.assertEquals("Mismatching build details found", build,
		builds.get(0));
    }

    @Test
    public void retrievalByNonMatchingFullNameShouldReturnEmptyList() {
	final BuildDetails build = RepositoryTests.createRandomBuildDetails();
	final Object buildId = repository.saveBuildDetails(build);
	Assert.assertNotNull("Unexpected null build id", buildId);

	final List<BuildDetails> builds = repository
	.getBuildDetailsByFullName(build.getFullName() + "NOMATCH");
	Assert.assertNotNull("Unexpected null list of builds", builds);
	Assert.assertTrue("Unexpected non-empty list of builds",
		builds.isEmpty());
    }

    @Test
    public void retrievalByMatchingFullNameShouldReturnNonEmptyList() {
	final BuildDetails build = RepositoryTests.createRandomBuildDetails();
	final Object buildId = repository.saveBuildDetails(build);
	Assert.assertNotNull("Unexpected null build id", buildId);

	final List<BuildDetails> builds = repository
	.getBuildDetailsByFullName(build.getFullName().toLowerCase());
	Assert.assertNotNull("Unexpected null list of builds", builds);
	Assert.assertFalse("Unexpected empty list of builds", builds.isEmpty());
	Assert.assertEquals("Unexpected number of builds", 1, builds.size());
	Assert.assertEquals("Mismatching build details found", build,
		builds.get(0));
    }

    @Test
    public void retrievalByNonMatchingNameShouldReturnEmptyList() {
	final BuildDetails build = RepositoryTests.createRandomBuildDetails();
	final Object buildId = repository.saveBuildDetails(build);
	Assert.assertNotNull("Unexpected null build id", buildId);

	final List<BuildDetails> builds = repository
	.getBuildDetailsByName(build.getName() + "NOMATCH");
	Assert.assertNotNull("Unexpected null list of builds", builds);
	Assert.assertTrue("Unexpected non-empty list of builds",
		builds.isEmpty());
    }

    @Test
    public void retrievalByMatchingNameShouldReturnNonEmptyList() {
	final BuildDetails build = RepositoryTests.createRandomBuildDetails();
	final Object buildId = repository.saveBuildDetails(build);
	Assert.assertNotNull("Unexpected null build id", buildId);

	final List<BuildDetails> builds = repository
	.getBuildDetailsByName(build.getName().toLowerCase());
	Assert.assertNotNull("Unexpected null list of builds", builds);
	Assert.assertFalse("Unexpected empty list of builds", builds.isEmpty());
	Assert.assertEquals("Unexpected number of builds", 1, builds.size());
	Assert.assertEquals("Mismatching build details found", build,
		builds.get(0));
    }

    @Test
    public void retrievalByNonMatchingUserIdShouldReturnEmptyList() {
	final BuildDetails build = RepositoryTests.createRandomBuildDetails();
	final Object buildId = repository.saveBuildDetails(build);
	Assert.assertNotNull("Unexpected null build id", buildId);

	final List<BuildDetails> builds = repository
	.getBuildDetailsByUserId(build.getUserId() + "NOMATCH");
	Assert.assertNotNull("Unexpected null list of builds", builds);
	Assert.assertTrue("Unexpected non-empty list of builds",
		builds.isEmpty());
    }

    @Test
    public void retrievalByMatchingUserIdShouldReturnNonEmptyList() {
	final BuildDetails build = RepositoryTests.createRandomBuildDetails();
	final Object buildId = repository.saveBuildDetails(build);
	Assert.assertNotNull("Unexpected null build id", buildId);

	final List<BuildDetails> builds = repository
	.getBuildDetailsByUserId(build.getUserId().toLowerCase());
	Assert.assertNotNull("Unexpected null list of builds", builds);
	Assert.assertFalse("Unexpected empty list of builds", builds.isEmpty());
	Assert.assertEquals("Unexpected number of builds", 1, builds.size());
	Assert.assertEquals("Mismatching build details found", build,
		builds.get(0));
    }

    @Test
    public void retrievalByNonMatchingUserNameShouldReturnEmptyList() {
	final BuildDetails build = RepositoryTests.createRandomBuildDetails();
	final Object buildId = repository.saveBuildDetails(build);
	Assert.assertNotNull("Unexpected null build id", buildId);

	final List<BuildDetails> builds = repository
	.getBuildDetailsByUserName(build.getUserName() + "NOMATCH");
	Assert.assertNotNull("Unexpected null list of builds", builds);
	Assert.assertTrue("Unexpected non-empty list of builds",
		builds.isEmpty());
    }

    @Test
    public void retrievalByMatchingUserNameShouldReturnNonEmptyList() {
	final BuildDetails build = RepositoryTests.createRandomBuildDetails();
	final Object buildId = repository.saveBuildDetails(build);
	Assert.assertNotNull("Unexpected null build id", buildId);

	final List<BuildDetails> builds = repository
	.getBuildDetailsByUserName(build.getUserName().toLowerCase());
	Assert.assertNotNull("Unexpected null list of builds", builds);
	Assert.assertFalse("Unexpected empty list of builds", builds.isEmpty());
	Assert.assertEquals("Unexpected number of builds", 1, builds.size());
	Assert.assertEquals("Mismatching build details found", build,
		builds.get(0));
    }

    @Test
    public void retrievalForMasterByMatchingDateRangeShouldReturnNonEmptyList() {
	final BuildDetails build = RepositoryTests.createRandomBuildDetails();
	final Object buildId = repository.saveBuildDetails(build);
	Assert.assertNotNull("Unexpected null build id", buildId);

	final List<BuildDetails> builds = repository
	.getBuildDetails(build.getNode().getMasterHostName(), new Date(
		0), build.getEndDate());
	Assert.assertNotNull("Unexpected null list of builds", builds);
	Assert.assertFalse("Unexpected empty list of builds", builds.isEmpty());
	Assert.assertEquals("Unexpected number of builds", 1, builds.size());
	Assert.assertEquals("Mismatching build details found", build,
		builds.get(0));
    }

    @Test
    public void retrievalForMasterAndProjectByMatchingDateRangeShouldReturnNonEmptyList() {
	final BuildDetails build = RepositoryTests.createRandomBuildDetails();
	final Object buildId = repository.saveBuildDetails(build);
	Assert.assertNotNull("Unexpected null build id", buildId);

	final List<BuildDetails> builds = repository.getBuildDetails(build
		.getNode().getMasterHostName(), build.getName(), new Date(0),
		build.getEndDate());
	Assert.assertNotNull("Unexpected null list of builds", builds);
	Assert.assertFalse("Unexpected empty list of builds", builds.isEmpty());
	Assert.assertEquals("Unexpected number of builds", 1, builds.size());
	Assert.assertEquals("Mismatching build details found", build,
		builds.get(0));
    }

    @Test
    public void updatedBuildDetailsShouldBePersisted() {
	final BuildDetails build = RepositoryTests.createRandomBuildDetails();
	final Object buildId = repository.saveBuildDetails(build);
	Assert.assertNotNull("Unexpected null build id", buildId);

	final String oldName = build.getName();
	final String newName = oldName + "UPDATED";

	build.setName(newName);
	repository.updateBuildDetails(build);

	List<BuildDetails> builds = repository.getBuildDetailsByName(oldName);
	Assert.assertNotNull("Unexpected null list of builds", builds);
	Assert.assertTrue("Unexpected non-empty list of builds",
		builds.isEmpty());

	builds = repository.getBuildDetailsByName(newName);
	Assert.assertNotNull("Unexpected null list of builds", builds);
	Assert.assertFalse("Unexpected empty list of builds", builds.isEmpty());
	Assert.assertEquals("Unexpected number of builds", 1, builds.size());
	Assert.assertEquals("Mismatching build details found", build,
		builds.get(0));
    }

    @Test
    public void updateNullBuildDetailsShouldFail() {
	try {
	    repository.updateBuildDetails(null);
	    Assert.fail("Unexpcted repository update for null object");
	} catch (final Exception e) {
	    Assert.assertEquals("Unexpected exception type",
		    IllegalArgumentException.class, e.getClass());
	}
    }

    @Test
    public void saveNullBuildDetailsShouldFail() {
	try {
	    repository.saveBuildDetails(null);
	    Assert.fail("Unexpected repository save for null object");
	} catch (final Exception e) {
	    Assert.assertEquals("Unexpected exception type",
		    IllegalArgumentException.class, e.getClass());
	}
    }

    @Test
    public void saveNullBuildDetailsListShouldFail() {
	try {
	    repository.saveBuildDetailsList(null);
	    Assert.fail("Unexpected repository save for null list");
	} catch (final Exception e) {
	    Assert.assertEquals("Unexpected exception type",
		    IllegalArgumentException.class, e.getClass());
	}
    }

    @Test
    public void retrievingAllProjectNamesShouldMatchDataset() {
	final Map<String, List<BuildDetails>> dataset = RepositoryTests
	.createRandomDataset(hostName);
	// ideally we should persist dataset in a transaction and roll it back
	// at the end of the test
	final TransactionStatus tx = txmgr.getTransaction(null);
	tx.setRollbackOnly();

	for (final List<BuildDetails> detailsList : dataset.values()) {
	    repository.saveBuildDetailsList(detailsList);
	}

	final Calendar fromDate = Calendar.getInstance();
	fromDate.add(Calendar.YEAR, -1);

	final Calendar toDate = Calendar.getInstance();

	final List<String> projectNames = repository.getProjectNames(hostName,
		fromDate.getTime(), toDate.getTime());

	txmgr.rollback(tx);

	Assert.assertNotNull("Unexpected null list of project names",
		projectNames);
	Assert.assertFalse("Unexpected empty list of project names",
		projectNames.isEmpty());
	Assert.assertEquals("Unexpected number of project names",
		dataset.size(), projectNames.size());
    }

    @Test
    public void retrievingMatchingProjectNameShouldReturnValidDataset() {
	final Map<String, List<BuildDetails>> dataset = RepositoryTests
	.createRandomDataset(hostName);
	// ideally we should persist dataset in a transaction and roll it back
	// at the end of the test
	final TransactionStatus tx = txmgr.getTransaction(null);
	tx.setRollbackOnly();

	for (final List<BuildDetails> detailsList : dataset.values()) {
	    repository.saveBuildDetailsList(detailsList);
	}

	final Calendar fromDate = Calendar.getInstance();
	fromDate.add(Calendar.YEAR, -1);

	final Calendar toDate = Calendar.getInstance();

	// let's try to find projects by matching the first one in the set
	final String projectName = dataset.keySet().iterator().next();

	// find exact match
	final List<String> projectNames = repository.getProjectNames(hostName,
		projectName, fromDate.getTime(), toDate.getTime());

	txmgr.rollback(tx);

	Assert.assertNotNull("Unexpected null list of project names",
		projectNames);
	Assert.assertFalse("Unexpected empty list of project names",
		projectNames.isEmpty());

	Assert.assertEquals("Unexpected number of project names", 1,
		projectNames.size());
    }

    @Test
    public void retrievingMatchingProjectNamePatternShouldReturnValidDataset() {
	final Map<String, List<BuildDetails>> dataset = RepositoryTests
	.createRandomDataset(hostName);
	// ideally we should persist dataset in a transaction and roll it back
	// at the end of the test
	final TransactionStatus tx = txmgr.getTransaction(null);
	tx.setRollbackOnly();

	for (final List<BuildDetails> detailsList : dataset.values()) {
	    repository.saveBuildDetailsList(detailsList);
	}

	final Calendar fromDate = Calendar.getInstance();
	fromDate.add(Calendar.YEAR, -1);

	final Calendar toDate = Calendar.getInstance();

	// let's try to find projects by matching the first one in the set
	final String projectName = dataset.keySet().iterator().next();

	// find partial match
	final String pattern = projectName.substring(0,
		projectName.length() / 2) + "%";

	final List<String> projectNames = repository.getProjectNames(hostName,
		pattern, fromDate.getTime(), toDate.getTime());

	txmgr.rollback(tx);

	Assert.assertNotNull("Unexpected null list of project names",
		projectNames);
	Assert.assertFalse("Unexpected empty list of project names",
		projectNames.isEmpty());
    }

    @Test
    public void retrievingOldProjectNamesShouldReturnEmptyList() {
	final Map<String, List<BuildDetails>> dataset = RepositoryTests
	.createRandomDataset(hostName);
	// ideally we should persist dataset in a transaction and roll it back
	// at the end of the test
	final TransactionStatus tx = txmgr.getTransaction(null);
	tx.setRollbackOnly();

	for (final List<BuildDetails> detailsList : dataset.values()) {
	    repository.saveBuildDetailsList(detailsList);
	}

	final Calendar fromDate = Calendar.getInstance();
	fromDate.add(Calendar.YEAR, -10);

	final Calendar toDate = Calendar.getInstance();
	toDate.add(Calendar.YEAR, -1);

	final List<String> projectNames = repository.getProjectNames(hostName,
		fromDate.getTime(), toDate.getTime());

	txmgr.rollback(tx);

	Assert.assertNotNull("Unexpected null list of project names",
		projectNames);
	Assert.assertTrue("Unexpected non-empty list of project names",
		projectNames.isEmpty());
    }

//    @Test
//    public void retrievalByNonMatchingParamsShouldReturnEmptyList() {
//		Assert.fail("Test not yet implemented");
//    }

    @Test
    public void retrievalByMatchingParamsShouldReturnNonEmptyList() {
	final Map<String, List<BuildDetails>> dataset = RepositoryTests
	.createRandomDataset(hostName);
	// ideally we should persist dataset in a transaction and roll it back
	// at the end of the test
	final TransactionStatus tx = txmgr.getTransaction(null);
	tx.setRollbackOnly();

	for (final List<BuildDetails> detailsList : dataset.values()) {
	    repository.saveBuildDetailsList(detailsList);
	}

	final Calendar fromDate = Calendar.getInstance();
	fromDate.add(Calendar.YEAR, -1);

	final Calendar toDate = Calendar.getInstance();

	final BuildDetails expected = dataset.entrySet().iterator().next()
	.getValue().get(0);
	final BuildParameter param = expected.getParameters().get(0);

	final List<BuildDetails> buildDetails = repository
	.getBuildDetailsByParams(hostName, param.getName(), param.getValue(),
		fromDate.getTime(), toDate.getTime());

	txmgr.rollback(tx);

	Assert.assertNotNull("Unexpected null list of project names",
		buildDetails);
	Assert.assertEquals("Unexpected number of build details retrieved", 1,
		buildDetails.size());
	Assert.assertEquals("Unexpected build details retrieved", expected,
		buildDetails.get(0));
    }
}
