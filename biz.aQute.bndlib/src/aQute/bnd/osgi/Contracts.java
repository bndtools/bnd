package aQute.bnd.osgi;

import static aQute.bnd.osgi.Constants.DUPLICATE_MARKER;

import java.util.Collection;
import java.util.Formatter;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import org.osgi.namespace.contract.ContractNamespace;
import org.osgi.resource.Namespace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import aQute.bnd.header.Attrs;
import aQute.bnd.header.Parameters;
import aQute.bnd.osgi.Descriptors.PackageRef;
import aQute.bnd.version.Version;
import aQute.lib.collections.MultiMap;
import aQute.service.reporter.Report.Location;

/**
 * OSGi Contracts are first defined in OSGi Enterprise Release 5.0.0. A Contract
 * is a namespace to control the versioning of a set of packages.
 *
 * @author aqute
 */
class Contracts {
	private final static Logger							logger					= LoggerFactory
		.getLogger(Contracts.class);

	private Analyzer									analyzer;
	private final MultiMap<PackageRef, Contract>		contracted				= new MultiMap<>(PackageRef.class,
		Contract.class, true);
	private MultiMap<Collection<Contract>, PackageRef>	overlappingContracts	= new MultiMap<>();
	private Instructions								instructions;
	private final Set<Contract>							contracts				= new HashSet<>();

	public class Contract {
		public String				name;
		public Attrs				decorators;
		public Collection<String>	uses;
		public Version				version;
		public String				from;

		@Override
		public String toString() {
			return "Contract [name=" + name + ";version=" + version + ";from=" + from + "]";
		}

	}

	public Contracts(Analyzer analyzer) {
		this.analyzer = analyzer;
	}

	Instructions getFilter() {
		if (instructions == null) {
			String contract = analyzer.getProperty(Constants.CONTRACT, "*");
			this.instructions = new Instructions(contract);
		}
		return instructions;
	}

	public void clear() {
		contracted.clear();
		overlappingContracts.clear();
		contracts.clear();
	}

	/**
	 * Collect contracts will take a domain and find any declared contracts.
	 * This happens early so that we have a list of contracts we can later
	 * compare the imports against.
	 */
	void collectContracts(String from, Parameters pcs) {
		logger.debug("collecting Contracts {} from {}", pcs, from);

		contract: for (Entry<String, Attrs> p : pcs.entrySet()) {
			String namespace = p.getKey();

			if (namespace.equals(ContractNamespace.CONTRACT_NAMESPACE)) {
				Attrs capabilityAttrs = p.getValue();

				String name = capabilityAttrs.get(ContractNamespace.CONTRACT_NAMESPACE);
				if (name == null) {
					analyzer.warning("No name (attr %s) defined in bundle %s from contract namespace: %s",
						ContractNamespace.CONTRACT_NAMESPACE, from, capabilityAttrs);
					continue contract;
				}

				for (Entry<Instruction, Attrs> i : getFilter().entrySet()) {
					Instruction instruction = i.getKey();
					if (instruction.matches(name)) {
						if (instruction.isNegated()) {
							logger.debug("{} rejected due to {}", namespace, instructions);
							continue contract;
						}

						logger.debug("accepted {} from {}", p, from);

						Contract c = new Contract();
						c.name = name;

						String list = capabilityAttrs.get(Namespace.CAPABILITY_USES_DIRECTIVE + ":");
						if (list == null || list.length() == 0) {
							analyzer.warning("Contract %s has no uses: directive in %s.", name, from);
							continue contract; // next contract
						}

						c.uses = Processor.split(list);

						try {
							Version version = capabilityAttrs.getTyped(Attrs.VERSION,
								ContractNamespace.CAPABILITY_VERSION_ATTRIBUTE);

							if (version != null)
								c.version = version;
						} catch (IllegalArgumentException iae) {
							// choose the highest version from the list
							List<Version> versions = capabilityAttrs.getTyped(Attrs.LIST_VERSION,
								ContractNamespace.CAPABILITY_VERSION_ATTRIBUTE);

							c.version = versions.get(0);

							for (Version version : versions) {
								if (version.compareTo(c.version) > 0) {
									c.version = version;
								}
							}
						}
						c.from = from;

						if (c.version == null) {
							c.version = Version.LOWEST;
							analyzer.warning("%s does not declare a version, assumed 0.0.0.", c);
						}
						c.decorators = new Attrs(i.getValue());

						//
						// Build up the package -> contract index
						//
						for (String pname : c.uses) {
							contracted.add(analyzer.getPackageRef(pname), c);
						}

						break;
					}
				}
			}
		}
	}

	/**
	 * Find out if a package is contracted. If there are multiple contracts for
	 * a package we remember this so we can generate a single error.
	 *
	 * @param packageRef
	 */
	boolean isContracted(PackageRef packageRef) {
		List<Contract> list = contracted.get(packageRef);
		if (list == null || list.isEmpty())
			return false;

		if (list.size() > 1) {

			//
			// There are multiple contracts trying to address
			// this package. We collect those so we can report them
			// as one error instead of one for each package
			//

			overlappingContracts.add(list, packageRef);
		}
		contracts.addAll(list);
		return true;
	}

	/**
	 * Called before we print the manifest. Should add any contracts that were
	 * actually used to the requirements.
	 *
	 * @param requirements
	 */
	void addToRequirements(Parameters requirements) {
		for (Contract c : contracts) {
			Attrs attrs = new Attrs(c.decorators);
			attrs.put(ContractNamespace.CONTRACT_NAMESPACE, c.name);
			String name = ContractNamespace.CONTRACT_NAMESPACE;
			while (requirements.containsKey(name)) {
				name += DUPLICATE_MARKER;
			}

			try (Formatter f = new Formatter()) {
				f.format("(&(%s=%s)(version=%s))", ContractNamespace.CONTRACT_NAMESPACE, c.name, c.version);

				// TODO : shall we also assert the attributes?

				attrs.put("filter:", f.toString());

				requirements.put(name, attrs);
			}
		}

		for (Entry<Collection<Contract>, List<PackageRef>> oc : overlappingContracts.entrySet()) {
			Location location = analyzer.error("Contracts %s declare the same packages in their uses: directive: %s. "
				+ "Contracts are found in declaring bundles (see their 'from' field), it is possible to control the finding"
				+ "with the -contract instruction", oc.getKey(), oc.getValue())
				.location();
			location.header = Constants.CONTRACT;
		}
	}

}
