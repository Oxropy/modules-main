package de.cubeisland.cubeengine.rulebook.bookManagement;

import de.cubeisland.cubeengine.core.bukkit.BookItem;
import static de.cubeisland.cubeengine.core.i18n.I18n._;
import de.cubeisland.cubeengine.core.i18n.Language;
import de.cubeisland.cubeengine.core.util.StringUtils;
import de.cubeisland.cubeengine.core.util.log.LogLevel;
import de.cubeisland.cubeengine.rulebook.Rulebook;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public final class RulebookManager 
{
    private final Rulebook module;
    
    private Map<String, String[]> rulebooks;
    
    public RulebookManager(Rulebook module)
    {
        this.module = module;
        
        this.rulebooks = new HashMap<String, String[]>();
        
        for(File book : this.module.getFolder().listFiles())
        {
            Set<Language> languages = this.module.getCore().getI18n().searchLanguages( StringUtils.stripFileExtention( book.getName() ) );
            
            if(languages.size() != 1)
            {
                this.module.getLogger().log(LogLevel.ERROR, "&cDo not know which language is meant");
            }
            else
            {
                Language language = languages.iterator().next();
                try 
                {
                    rulebooks.put(language.getName(), RuleBookFile.convertToPages(book));
                }
                catch (IOException ex) 
                {
                    this.module.getLogger().log(LogLevel.ERROR, "Can't read the file {0}", book.getName());
                }
            }
        }
    }
    
    public Collection<String> getLanguages()
    {
        return this.rulebooks.keySet();
    }
    
    public boolean contains(String language)
    {
        return this.contains(language, 2);
    }
    
    public boolean contains(String language, int editDistance)
    {
        Set<Language> languages = this.module.getCore().getI18n().searchLanguages(language, editDistance);
        if(languages.size() == 1 && this.rulebooks.containsKey(languages.iterator().next().getName()))
        {
            return true;
        }
        return false;
    }
    
    public String[] getPages(String language)
    {
        return this.getPages(language, 2);
    }
    
    public String[] getPages(String language, int editDistance)
    {
        Set<Language> languages = this.module.getCore().getI18n().searchLanguages(language, editDistance);
        if(languages.size() == 1)
        {
            return this.rulebooks.get(languages.iterator().next().getName());
        }
        return null;
    }
    
    public ItemStack getBook(String language)
    {
        if(this.contains(language))
        {
            BookItem rulebook = new BookItem(new ItemStack(Material.WRITTEN_BOOK));

            rulebook.setAuthor(Bukkit.getServerName());
            rulebook.setTitle(_(language, "rulebook", "Rulebook"));
            rulebook.setPages(this.getPages(language));
            rulebook.setTag("rulebook", true);
            rulebook.setTag("language", language);
            
            return rulebook.getItemStack();
        }
        return null;
    }
    
    public void addBook(ItemStack book, String language)
    {
        Set<Language> languages = this.module.getCore().getI18n().searchLanguages(language);
        if(!this.contains(language) && languages.size() != 1)
        {
            Language lang = languages.iterator().next();
            BookItem item = new BookItem(book);
            try 
            {
                File file = new File(this.module.getFolder().getAbsoluteFile(), lang + ".txt");
                RuleBookFile.createFile(file, item.getPages());
                
                this.rulebooks.put(language, RuleBookFile.convertToPages(file));
            } 
            catch (IOException ex) 
            {
                this.module.getLogger().log(LogLevel.ERROR, "Error by creating the book");
            }
        }
    }
}