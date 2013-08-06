package aQute.bnd.osgi;

import java.util.*;
import java.util.Map.Entry;

import org.osgi.namespace.contract.*;

import aQute.bnd.header.*;
import aQute.bnd.osgi.Descriptors.PackageRef;
import aQute.bnd.version.*;
import aQute.lib.collections.*;
import aQute.service.reporter.Report.Location;

/**
 * OSGi Contracts are first defined in OSGi Enterprise Release 5.0.0. A Contract
 * is a namespace to control the versioning of a set of packages.
 * 
 * @author aqute
 */
class Contracts {

	private Analyzer									analyzer;
	private final MultiMap<PackageRef,Contract>			contracted				= new MultiMap<PackageRef,Contract>(
																						PackageRef.class,
																						Contract.class, true);
	private MultiMap<Collection<Contract>,PackageRef>	overlappingContracts	= new MultiMap<Collection<Contract>,PackageRef>();
	private Instructions								instructions;
	private final Set<Contract>							contracts				= new HashSet<Contract>();

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
			String contract = analyzer.getProperty(Constants.CONTRACT);
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

		contract: for (Entry<String,Attrs> p : pcs.entrySet()) {
			String namespace = p.getKey();

			if (namespace.equals(ContractNamespace.CONTRACT_NAMESPACE)) {
				Attrs capabilityAttrs = p.getValue();

				String name = capabilityAttrs.get(ContractNamespace.CONTRACT_NAMESPACE);
				if (name == null) {
					analyzer.warning("No name (attr %s) defined in bundle %s from contract namespace: %s",
							ContractNamespace.CONTRACT_NAMESPACE, from, capabilityAttrs);
					continue contract;
				}

				for (Entry<Instruction,Attrs> i : getFilter().entrySet()) {
					Instruction instruction = i.getKey();
					if (instruction.matches(name)) {
						if (instruction.isNegated()) {
							analyzer.trace("%s rejected due to %s", namespace, instructions);
							continue contract;
						}

						analyzer.trace("accepted %s", p);

						Contract c = new Contract();
						c.name = name;

						String list = capabilityAttrs.get(ContractNamespace.CAPABILITY_USES_DIRECTIVE + ":");
						if (list == null || list.length() == 0) {
							analyzer.warning("Contract %s has no uses: directive in %s.", name, from);
							continue contract; // next contract
						}

						c.uses = Processor.split(list);

						c.version = (Version) capabilityAttrs.getTyped(ContractNamespace.CAPABILITY_VERSION_ATTRIBUTE);
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
	 * @return
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
			String range = analyzer.applyVersionPolicy(c.version.toString(), c.decorators.getVersion(), false);
			String name = ContractNamespace.CONTRACT_NAMESPACE;
			while (requirements.containsKey(name))
				name += "~";

			VersionRange r = new VersionRange(range);

			Formatter f = new Formatter();
			try {
				f.format("(&(%s=%s)%s)", ContractNamespace.CONTRACT_NAMESPACE, c.name, r.toFilter());

				// TODO : shall we also assert the attributes?

				attrs.put("filter:", f.toString());

				requirements.put(name, attrs);
			}
			finally {
				f.close();
			}
		}

		for (Entry<Collection<Contract>,List<PackageRef>> oc : overlappingContracts.entrySet()) {
			Location location = analyzer
					.error("Contracts %s declare the same packages in their uses: directive: %s. "
							+ "Contracts are found in declaring bundles (see their 'from' field), it is possible to control the finding"
							+ "with the -contract instruction", oc.getKey(), oc.getValue()).location();
			location.header = Constants.CONTRACT;
		}
	}

}
