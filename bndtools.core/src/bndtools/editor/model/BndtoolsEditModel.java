/*******************************************************************************
 * Copyright (c) 2010 Neil Bartlett.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Neil Bartlett - initial API and implementation
 *******************************************************************************/
package bndtools.editor.model;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.eclipse.jface.text.IDocument;

import aQute.bnd.build.model.BndEditModel;
import aQute.bnd.build.model.clauses.VersionedClause;
import aQute.bnd.build.model.conversions.CollectionFormatter;
import aQute.bnd.build.model.conversions.Converter;
import aQute.bnd.build.model.conversions.EnumConverter;
import aQute.bnd.build.model.conversions.EnumFormatter;
import aQute.bnd.build.model.conversions.NoopConverter;
import aQute.bnd.build.model.conversions.PropertiesConverter;
import aQute.bnd.build.model.conversions.SimpleListConverter;
import bndtools.BndConstants;
import bndtools.api.EE;
import bndtools.api.IPersistableBndModel;
import bndtools.api.Requirement;
import bndtools.api.ResolveMode;

/**
 * A model for a Bnd file. In the first iteration, use a simple Properties object; this will need to be enhanced to
 * additionally record formatting, e.g. line breaks and empty lines, and comments.
 * 
 * @author Neil Bartlett
 */
public class BndtoolsEditModel extends BndEditModel implements IPersistableBndModel {

    private static final String[] LOCAL_PROPERTIES = new String[] {
            BndConstants.RUNFRAMEWORK, aQute.lib.osgi.Constants.RUNVM, BndConstants.RUNREQUIRE, BndConstants.RUNEE, BndConstants.RUNREPOS, BndConstants.RESOLVE_MODE
    };
    static {
        String[] merged = new String[KNOWN_PROPERTIES.length + LOCAL_PROPERTIES.length];
        System.arraycopy(KNOWN_PROPERTIES, 0, merged, 0, KNOWN_PROPERTIES.length);
        System.arraycopy(LOCAL_PROPERTIES, 0, merged, KNOWN_PROPERTIES.length, LOCAL_PROPERTIES.length);
    }

    // CONVERTERS
    Converter<Map<String,String>,String> propertiesConverter = new PropertiesConverter();

    Converter<List<Requirement>,String> requirementListConverter = SimpleListConverter.create(new Converter<Requirement,String>() {
        public Requirement convert(String input) throws IllegalArgumentException {
            int index = input.indexOf(":");
            if (index < 0)
                throw new IllegalArgumentException("Invalid format for OBR requirement");

            String name = input.substring(0, index);
            String filter = input.substring(index + 1);

            return new Requirement(name, filter);
        }
    });
    Converter<EE,String> eeConverter = new Converter<EE,String>() {
        public EE convert(String input) throws IllegalArgumentException {
            return EE.parse(input);
        }
    };

    Converter<ResolveMode,String> resolveModeConverter = EnumConverter.create(ResolveMode.class, ResolveMode.manual);

    // FORMATTERS
    Converter<String,Collection< ? extends Requirement>> requirementListFormatter = new CollectionFormatter<Requirement>(LIST_SEPARATOR, new Converter<String,Requirement>() {
        public String convert(Requirement input) throws IllegalArgumentException {
            return new StringBuilder().append(input.getName()).append(':').append(input.getFilter()).toString();
        }
    }, null);
    Converter<String,EE> eeFormatter = new Converter<String,EE>() {
        public String convert(EE input) throws IllegalArgumentException {
            return input != null ? input.getEEName() : null;
        }
    };
    Converter<String,Collection< ? extends String>> runReposFormatter = new CollectionFormatter<String>(LIST_SEPARATOR, aQute.lib.osgi.Constants.EMPTY_HEADER);
    Converter<String,ResolveMode> resolveModeFormatter = EnumFormatter.create(ResolveMode.class, ResolveMode.manual);

    public BndtoolsEditModel() {
        // register converters
        converters.put(aQute.lib.osgi.Constants.RUNPROPERTIES, propertiesConverter);
        converters.put(BndConstants.RUNREQUIRE, requirementListConverter);
        converters.put(BndConstants.RUNEE, new NoopConverter<String>());
        converters.put(BndConstants.RESOLVE_MODE, resolveModeConverter);

        formatters.put(BndConstants.RUNREQUIRE, requirementListFormatter);
        formatters.put(BndConstants.RUNEE, new NoopConverter<String>());
        formatters.put(BndConstants.RUNREPOS, runReposFormatter);
        formatters.put(BndConstants.RESOLVE_MODE, resolveModeFormatter);
    }

    public void loadFrom(IDocument document) throws IOException {
        super.loadFrom(new ByteArrayInputStream(document.get().getBytes(ISO_8859_1)));
    }

    public void saveChangesTo(IDocument document) {
        IDocumentWrapper documentWrapper = new IDocumentWrapper(document);
        super.saveChangesTo(documentWrapper);
    }

    public List<VersionedClause> getBackupRunBundles() {
        return doGetObject(BndConstants.BACKUP_RUNBUNDLES, clauseListConverter);
    }

    public void setBackupRunBundles(List< ? extends VersionedClause> paths) {
        List<VersionedClause> oldValue = getBuildPath();
        doSetObject(BndConstants.BACKUP_RUNBUNDLES, oldValue, paths, headerClauseListFormatter);
    }

    public String getRunFramework() {
        return doGetObject(BndConstants.RUNFRAMEWORK, stringConverter);
    }

    public void setRunFramework(String clause) {
        String oldValue = getRunFramework();
        doSetObject(BndConstants.RUNFRAMEWORK, oldValue, clause, newlineEscapeFormatter);
    }

    public List<Requirement> getRunRequire() {
        return doGetObject(BndConstants.RUNREQUIRE, requirementListConverter);
    }

    public void setRunRequire(List<Requirement> requires) {
        List<Requirement> old = getRunRequire();
        doSetObject(BndConstants.RUNREQUIRE, old, requires, requirementListFormatter);
    }

    public ResolveMode getResolveMode() {
        return doGetObject(BndConstants.RESOLVE_MODE, resolveModeConverter);
    }

    public void setResolveMode(ResolveMode mode) {
        ResolveMode old = getResolveMode();
        doSetObject(BndConstants.RESOLVE_MODE, old, mode, resolveModeFormatter);
    }

    public EE getEE() {
        return doGetObject(BndConstants.RUNEE, eeConverter);
    }

    public void setEE(EE ee) {
        EE old = getEE();
        doSetObject(BndConstants.RUNEE, old, ee, eeFormatter);
    }

    public List<String> getRunRepos() {
        return doGetObject(BndConstants.RUNREPOS, listConverter);
    }

    public void setRunRepos(List<String> repos) {
        List<String> old = getRunRepos();
        doSetObject(BndConstants.RUNREPOS, old, repos, runReposFormatter);
    }
}