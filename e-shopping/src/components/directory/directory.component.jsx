import React from 'react';
import { connect } from 'react-redux';
import sections from './directory.data';
import './directory.styles.scss'
import MenuItem from '../menu-item/menu-item.component';
import { createStructuredSelector } from 'reselect';
import { selectDirectorySection } from '../../redux/directory/directory.selectors';

const Directory = ({ sections }) => (

      <div className="directory-menu">
        {
          sections.map(({id, ...sectionProps}) => (
            <MenuItem key={id} {...sectionProps} />
          ))
        }
      </div>
)

const mapStateToProps = state => createStructuredSelector ({
  sections: selectDirectorySection
})





export default connect(mapStateToProps, null)(Directory);
